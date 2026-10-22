package media.toloka.rfa.media.messanger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.media.messanger.model.ChatPresenceEvent;
import media.toloka.rfa.media.messanger.model.MessageRoom;
import media.toloka.rfa.media.messanger.model.enumerate.EChatRoomType;
import media.toloka.rfa.media.messanger.repository.MessageRoomRepository;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.radio.repository.StationRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BroadcastRoomService {

    private final StationRepo stationRepo;
    private final MessageRoomRepository messageRoomRepository;
    private final ChatPresenceService chatPresenceService;

    /**
     * Ліниве створення кімнат для прямого ефіру.
     * Якщо кімнати вже створені, повертає їхні UUID (ідемпотентність).
     * Якщо ні — створює нові записи в MessageRoom і прив'язує до Station.
     */
    @Transactional
    public Map<String, String> prepareBroadcastRooms(String stationUuid) {
        Station station = stationRepo.getStationByUuid(stationUuid);
        if (station == null) {
            throw new IllegalArgumentException("Радіостанцію з UUID " + stationUuid + " не знайдено");
        }

        // Перевірка: якщо кімнати вже створені, повертаємо їх
        if (station.getGreenRoomUuid() != null && station.getBroadcastRoomUuid() != null && station.getStaffRoomUuid() != null) {
            log.info("Кімнати для прямого ефіру станції {} вже існують. Повертаємо існуючі UUID.", stationUuid);
            return buildResponseMap(station);
        }

        log.info("Створення нових кімнат для прямого ефіру станції: {}", station.getName());

        // 1. Створюємо Green Room
        MessageRoom greenRoom = createAndSaveRoom("🟡 Green Room: " + station.getName(), EChatRoomType.CHATROOM_TYPE_GREEN);

        // 2. Створюємо Broadcast Room (Ефір)
        MessageRoom broadcastRoom = createAndSaveRoom("🔴 Ефір: " + station.getName(), EChatRoomType.CHATROOM_TYPE_BROADCAST);

        // 3. Створюємо Staff Room (Персонал)
        MessageRoom staffRoom = createAndSaveRoom("🔒 Staff Room: " + station.getName(), EChatRoomType.CHATROOM_TYPE_STAFF);

        // 4. Прив'язуємо UUID до станції
        station.setGreenRoomUuid(greenRoom.getUuid());
        station.setBroadcastRoomUuid(broadcastRoom.getUuid());
        station.setStaffRoomUuid(staffRoom.getUuid());

        stationRepo.save(station);

        // 5. Сповіщаємо всі вузли кластера через RabbitMQ про нові кімнати
        notifyClusterAboutNewRoom(greenRoom);
        notifyClusterAboutNewRoom(broadcastRoom);
        notifyClusterAboutNewRoom(staffRoom);

        log.info("✅ Кімнати успішно створено та прив'язано до станції {}", stationUuid);
        return buildResponseMap(station);
    }

    private MessageRoom createAndSaveRoom(String name, EChatRoomType type) {
        MessageRoom room = new MessageRoom();
        room.setRoomname(name);
        room.setRoomtype(type);
        room.setRoomOnlineStatus(false); // Поки що не в онлайні
        return messageRoomRepository.save(room);
    }

    private void notifyClusterAboutNewRoom(MessageRoom room) {
        chatPresenceService.publishPresenceEvent(
                ChatPresenceEvent.EventType.ROOM_UPDATED,
                room.getUuid(),
                room.getRoomname()
        );
    }

    private Map<String, String> buildResponseMap(Station station) {
        Map<String, String> result = new HashMap<>();
        result.put("stationUuid", station.getUuid());
        result.put("greenRoomUuid", station.getGreenRoomUuid());
        result.put("broadcastRoomUuid", station.getBroadcastRoomUuid());
        result.put("staffRoomUuid", station.getStaffRoomUuid());
        return result;
    }
}