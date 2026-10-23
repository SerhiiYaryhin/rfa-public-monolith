package media.toloka.rfa.media.messanger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.media.messanger.model.ChatPresenceEvent;
import media.toloka.rfa.media.messanger.model.MessageRoom;
import media.toloka.rfa.media.messanger.model.enumerate.EChatRoomType;
import media.toloka.rfa.media.messanger.repository.MessageRoomRepository;
import media.toloka.rfa.media.webrtc.service.v2.JanusRoomService;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.radio.repository.StationRepo;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервіс підготовки кімнат прямого ефіру.
 *
 * Відповідає за:
 *  1. Ліниве створення трьох текстових кімнат (Green/Broadcast/Staff) у БД
 *     та прив'язку їх UUID до Station.
 *  2. Дзеркалювання цих кімнат у Janus AudioBridge (MessageRoom.id = Janus room id).
 *  3. Self-healing: якщо Janus перезапустився і втратив динамічні кімнати,
 *     сервіс автоматично перестворює їх (при старті додатку, періодично
 *     та за явним запитом через REST /api/v1/broadcast/sync).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BroadcastRoomService {

    private final StationRepo stationRepo;
    private final MessageRoomRepository messageRoomRepository;
    private final ChatPresenceService chatPresenceService;
    private final JanusRoomService janusRoomService;

    /**
     * Ліниве створення кімнат для прямого ефіру.
     * Якщо кімнати вже створені — повертає їхні UUID і додатково переконується,
     * що вони присутні в Janus (self-healing після рестарту медіасервера).
     *
     * @param stationUuid UUID радіостанції
     * @return мапа з UUID створених кімнат
     */
    @Transactional
    public Map<String, String> prepareBroadcastRooms(String stationUuid) {
        Station station = stationRepo.getStationByUuid(stationUuid);
        if (station == null) {
            throw new IllegalArgumentException("Радіостанцію з UUID " + stationUuid + " не знайдено");
        }

        // ШЛЯХ А: кімнати вже створені раніше (ідемпотентність).
        if (station.getGreenRoomUuid() != null
                && station.getBroadcastRoomUuid() != null
                && station.getStaffRoomUuid() != null) {
            log.info("Кімнати станції {} вже існують. Перевіряємо їх наявність у Janus...", stationUuid);
            // Self-healing: Janus міг перезапуститися і втратити кімнати з пам'яті.
            // Не строга перевірка: якщо Janus недоступний — не блокуємо модератора.
            ensureJanusRooms(station, false);
            return buildResponseMap(station);
        }

        // ШЛЯХ Б: перше створення кімнат.
        log.info("🎙️ Починаємо створення кімнат для прямого ефіру станції: {}", station.getName());
        try {
            // 1. Створюємо Green Room (кімната підготовки гостей)
            MessageRoom greenRoom = createAndSaveRoom(
                    "🟡 Green Room: " + station.getName(),
                    EChatRoomType.CHATROOM_TYPE_GREEN
            );
            // 2. Створюємо Broadcast Room (основна кімната ефіру)
            MessageRoom broadcastRoom = createAndSaveRoom(
                    "🔴 Ефір: " + station.getName(),
                    EChatRoomType.CHATROOM_TYPE_BROADCAST
            );
            // 3. Створюємо Staff Room (кімната технічного персоналу)
            MessageRoom staffRoom = createAndSaveRoom(
                    "🔒 Staff Room: " + station.getName(),
                    EChatRoomType.CHATROOM_TYPE_STAFF
            );

            // 4. Прив'язуємо UUID до станції
            station.setGreenRoomUuid(greenRoom.getUuid());
            station.setBroadcastRoomUuid(broadcastRoom.getUuid());
            station.setStaffRoomUuid(staffRoom.getUuid());
            stationRepo.save(station);

            // 5. Створюємо кімнати в Janus. Якщо Janus недоступний — транзакція
            //    БД відкочується (строга перевірка), щоб не лишити битих посилань.
            ensureJanusRooms(station, true);

            // 6. Сповіщаємо всі вузли кластера через RabbitMQ про нові кімнати
            notifyClusterAboutNewRoom(greenRoom);
            notifyClusterAboutNewRoom(broadcastRoom);
            notifyClusterAboutNewRoom(staffRoom);

            log.info("✅ Кімнати успішно створено: Green={}, Broadcast={}, Staff={}",
                    greenRoom.getId(), broadcastRoom.getId(), staffRoom.getId());
            return buildResponseMap(station);
        } catch (Exception e) {
            log.error("❌ Помилка при створенні кімнат для станції {}: {}", stationUuid, e.getMessage(), e);
            throw new RuntimeException("Не вдалося створити кімнати для ефіру: " + e.getMessage(), e);
        }
    }

    /**
     * Примусова синхронізація кімнат станції з Janus (викликається з REST).
     * Перестворює усі відсутні кімнати; помилка Janus = помилка операції.
     */
    @Transactional(readOnly = true)
    public Map<String, String> syncJanusRooms(String stationUuid) {
        Station station = stationRepo.getStationByUuid(stationUuid);
        if (station == null) {
            throw new IllegalArgumentException("Радіостанцію з UUID " + stationUuid + " не знайдено");
        }
        ensureJanusRooms(station, true);
        return buildResponseMap(station);
    }

    /**
     * Self-healing при старті додатку: переконуємось, що кімнати всіх станцій,
     * для яких ефір вже готувався, присутні в Janus.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void healOnStartup() {
        log.info(" Self-healing: перевірка кімнат ефіру у Janus після старту додатку...");
        healAllStations();
    }

    /**
     * Періодичний self-healing (кожні 60 секунд): захищає від ситуації,
     * коли Janus перезапустився під час роботи порталу.
     * Використовуємо лише create + ігнорування "вже існує", тому операція легка.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void healPeriodically() {
        try {
            healAllStations();
        } catch (Exception e) {
            log.debug("Періодична перевірка кімнат Janus завершилася з помилкою: {}", e.getMessage());
        }
    }

    /**
     * Проходить по всіх станціях з підготовленими кімнатами і долікує відсутні
     * кімнати в Janus.
     */
    private void healAllStations() {
        List<Station> stations = stationRepo.findAll();
        for (Station station : stations) {
            if (station.getGreenRoomUuid() == null
                    && station.getBroadcastRoomUuid() == null
                    && station.getStaffRoomUuid() == null) {
                continue; // станція ще не готувала ефір
            }
            try {
                ensureJanusRooms(station, false);
            } catch (Exception e) {
                log.warn("⚠ Self-healing для станції {} не вдався: {}", station.getUuid(), e.getMessage());
            }
        }
    }

    /**
     * Гарантує присутність трьох кімнат станції в Janus AudioBridge.
     *
     * @param strict true  — помилка Janus кидає виняток (первинне створення);
     *               false — помилки лише логуються (self-healing у фоні).
     */
    private void ensureJanusRooms(Station station, boolean strict) {
        String[] uuids = {
                station.getGreenRoomUuid(),
                station.getBroadcastRoomUuid(),
                station.getStaffRoomUuid()
        };
        for (String uuid : uuids) {
            if (uuid == null) continue;
            MessageRoom room = messageRoomRepository.getByUuid(uuid);
            if (room == null) {
                log.warn("⚠ Кімнату з UUID {} не знайдено в БД — пропускаємо.", uuid);
                continue;
            }
            createRoomInJanusTolerant(room, strict);
        }
    }

    /**
     * Створює кімнату в Janus; якщо кімната вже існує — мовчки пропускає.
     * Саме ця толерантність робить операцію ідемпотентною.
     */
    private void createRoomInJanusTolerant(MessageRoom room, boolean strict) {
        try {
            janusRoomService.createAudioBridgeRoom(room.getId(), room.getRoomname());
            log.info("✅ Кімната Janus створена/оновлена: ID={}, Name={}", room.getId(), room.getRoomname());
        } catch (Exception e) {
            String msg = (e.getMessage() == null) ? "" : e.getMessage().toLowerCase();
            if (msg.contains("exist") || msg.contains("already")) {
                // Кімната вже є в Janus — це нормальний стан для ідемпотентного виклику
                log.debug("Кімната Janus ID={} вже існує — пропускаємо.", room.getId());
            } else if (strict) {
                throw new RuntimeException("Не вдалося створити кімнату в Janus: " + room.getRoomname(), e);
            } else {
                log.error("⚠ Не вдалося перестворити кімнату Janus ID={}: {}", room.getId(), e.getMessage());
            }
        }
    }

    /**
     * Створює та зберігає запис кімнати в базі даних.
     */
    private MessageRoom createAndSaveRoom(String name, EChatRoomType type) {
        MessageRoom room = new MessageRoom();
        room.setRoomname(name);
        room.setRoomtype(type);
        room.setRoomOnlineStatus(false); // Поки що не в онлайні
        MessageRoom savedRoom = messageRoomRepository.save(room);
        log.debug("Кімнату збережено в БД: ID={}, UUID={}, Name={}, Type={}",
                savedRoom.getId(), savedRoom.getUuid(), savedRoom.getRoomname(), savedRoom.getRoomtype());
        return savedRoom;
    }

    /**
     * Сповіщає кластер про нову/оновлену кімнату через RabbitMQ.
     */
    private void notifyClusterAboutNewRoom(MessageRoom room) {
        chatPresenceService.publishPresenceEvent(
                ChatPresenceEvent.EventType.ROOM_UPDATED,
                room.getUuid(),
                room.getRoomname()
        );
        log.debug("Сповіщення кластера про кімнату: UUID={}, Name={}", room.getUuid(), room.getRoomname());
    }

    /**
     * Будує мапу відповіді для REST-контролера.
     * УВАГА: ключі без пробілів — саме за ними фронтенд читає дані!
     */
    private Map<String, String> buildResponseMap(Station station) {
        Map<String, String> result = new HashMap<>();
        result.put("stationUuid", station.getUuid());
        result.put("stationName", station.getName());
        result.put("greenRoomUuid", station.getGreenRoomUuid());
        result.put("broadcastRoomUuid", station.getBroadcastRoomUuid());
        result.put("staffRoomUuid", station.getStaffRoomUuid());
        return result;
    }
}