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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Сервіс підготовки кімнат прямого ефіру.
 * Створює кімнати в БД (MessageRoom) та дзеркалить їх у Janus AudioBridge.
 * UUID кімнат зберігаються у Station (greenRoomUuid / broadcastRoomUuid / staffRoomUuid),
 * а числовий MessageRoom.id використовується як ID кімнати в Janus.
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
     * Ліниве (ідімпотентне) створення кімнат прямого ефіру.
     * Якщо кімнати вже прив'язані до станції — лише переконуємось, що вони
     * присутні в Janus (self-healing після рестарту Janus), і повертаємо UUID.
     */
    @Transactional
    public Map<String, String> prepareBroadcastRooms(String stationUuid) {
        Station station = stationRepo.getStationByUuid(stationUuid);
        if (station == null) {
            throw new IllegalArgumentException("Радіостанцію з UUID " + stationUuid + " не знайдено");
        }

        // ШЛЯХ А: кімнати вже створені раніше.
        if (station.getGreenRoomUuid() != null
                && station.getBroadcastRoomUuid() != null
                && station.getStaffRoomUuid() != null) {
            log.info("Кімнати станції {} вже існують. Перевіряємо їх наявність у Janus...", stationUuid);
            // SELF-HEALING: Janus міг перезапуститися і втратити кімнати з пам'яті.
            // Достоворюємо відсутні кімнати, але НЕ падаємо, якщо Janus недоступний —
            // інакше модератор не зможе відкрити панель під час проблем з медіасервером.
            ensureJanusRooms(station, false);
            return buildResponseMap(station);
        }

        // ШЛЯХ Б: перше створення. Тут помилка Janus = критична (котимо транзакцію).
        log.info("🎙️ Починаємо створення кімнат для прямого ефіру станції: {}", station.getName());
        try {
            MessageRoom greenRoom = createAndSaveRoom("🟡 Green Room: " + station.getName(), EChatRoomType.CHATROOM_TYPE_GREEN);
            MessageRoom broadcastRoom = createAndSaveRoom("🔴 Ефір: " + station.getName(), EChatROOM_TYPE_BROADCAST_FIX);
            MessageRoom staffRoom = createAndSaveRoom("🔒 Staff Room: " + station.getName(), EChatRoomType.CHATROOM_TYPE_STAFF);

            // Прив'язуємо UUID до станції ДО викликів Janus, щоб у разі відкату
            // не залишити станцію з битими посиланнями.
            station.setGreenRoomUuid(greenRoom.getUuid());
            station.setBroadcastRoomUuid(broadcastRoom.getUuid());
            station.setStaffRoomUuid(staffRoom.getUuid());
            stationRepo.save(station);

            // Створюємо кімнати в Janus. Якщо Janus впаде посередині — транзакція БД
            // відкотиться, а "осиротілі" кімнати в Janus прибере RoomSynchronizationService.
            ensureJanusRooms(station, true);

            // Сповіщаємо всі вузли кластера чату про нові кімнати.
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
     * Створює та зберігає запис кімнати в БД.
     * Повертає сутність зі згенерованим id (він стане Janus room id).
     */
    private MessageRoom createAndSaveRoom(String name, EChatRoomType type) {
        MessageRoom room = new MessageRoom();
        room.setRoomname(name);
        room.setRoomtype(type);
        room.setRoomOnlineStatus(false);
        MessageRoom saved = messageRoomRepository.save(room);
        log.debug("Кімнату збережено в БД: ID={}, UUID={}, Type={}", saved.getId(), saved.getUuid(), type);
        return saved;
    }

    /**
     * Гарантує наявність трьох кімнат станції в Janus AudioBridge.
     *
     * @param strict true  — помилка Janus кидає виняток (первинне створення);
     *               false — помилки лише логуються (self-healing на відкритті панелі).
     */
    private void ensureJanusRooms(Station station, boolean strict) {
        String[] uuids = {station.getGreenRoomUuid(), station.getBroadcastRoomUuid(), station.getStaffRoomUuid()};
        for (String uuid : uuids) {
            if (uuid == null) continue;
            MessageRoom room = messageRoomRepository.getByUuid(uuid);
            if (room == null) continue;
            try {
                janusRoomService.createAudioBridgeRoom(room.getId(), room.getRoomname());
                log.info("✅ Кімната Janus створена/оновлена: ID={}", room.getId());
            } catch (Exception e) {
                // Janus повертає помилку "Room already exists", якщо кімната вже є —
                // це НОРМАЛЬНО для ідемпотентного виклику, тому ігноруємо такий випадок.
                if (e.getMessage() != null && e.getMessage().toLowerCase().contains("exist")) {
                    log.debug("Кімната Janus ID={} вже існує — пропускаємо.", room.getId());
                } else if (strict) {
                    throw e; // первинне створення: без Janus немає сенсу продовжувати
                } else {
                    log.error("⚠ Не вдалося перестворити кімнату Janus ID={}: {}", room.getId(), e.getMessage());
                }
            }
        }
    }

    /** Сповіщення кластера чату про нову/оновлену кімнату через RabbitMQ. */
    private void notifyClusterAboutNewRoom(MessageRoom room) {
        chatPresenceService.publishPresenceEvent(
                ChatPresenceEvent.EventType.ROOM_UPDATED,
                room.getUuid(),
                room.getRoomname()
        );
    }

    /**
     * Мапа відповіді для REST-контролера.
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