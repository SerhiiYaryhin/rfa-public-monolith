package media.toloka.rfa.media.messanger.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ChatRoomPresenceService {

    private final Map<String, Set<String>> roomParticipants = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userRooms = new ConcurrentHashMap<>();

    public void addUserToRoom(String userIdentifier, String roomUuid) {
        roomParticipants.computeIfAbsent(roomUuid, k -> ConcurrentHashMap.newKeySet())
                .add(userIdentifier);
        userRooms.computeIfAbsent(userIdentifier, k -> ConcurrentHashMap.newKeySet())
                .add(roomUuid);
        log.debug("✅ Додано {} в кімнату {}", userIdentifier, roomUuid);
    }

    public void removeUserFromRoom(String userIdentifier, String roomUuid) {
        Set<String> participants = roomParticipants.get(roomUuid);
        if (participants != null) {
            participants.remove(userIdentifier);
            if (participants.isEmpty()) {
                roomParticipants.remove(roomUuid);
            }
        }
        Set<String> rooms = userRooms.get(userIdentifier);
        if (rooms != null) {
            rooms.remove(roomUuid);
            if (rooms.isEmpty()) {
                userRooms.remove(userIdentifier);
            }
        }
        log.debug("❌ Видалено {} з кімнати {}", userIdentifier, roomUuid);
    }

    public void removeUserFromAllRooms(String userIdentifier) {
        Set<String> rooms = userRooms.remove(userIdentifier);
        if (rooms != null) {
            for (String roomUuid : rooms) {
                removeUserFromRoom(userIdentifier, roomUuid);
            }
        }
        log.debug("👋 Видалено {} з усіх кімнат", userIdentifier);
    }

    public Set<String> getRoomParticipants(String roomUuid) {
        return roomParticipants.getOrDefault(roomUuid, Collections.emptySet());
    }

    public Set<String> getUserRooms(String userIdentifier) {
        return userRooms.getOrDefault(userIdentifier, Collections.emptySet());
    }

    /**
     * ✅ НОВИЙ МЕТОД: Замінює всі входження email на UUID у всіх кімнатах.
     * Викликається з /app/hello, коли стає відомий справжній UUID користувача.
     */
    public void replaceEmailWithUuid(String email, String uuid) {
        if (email == null || uuid == null || email.equals(uuid)) {
            return;
        }

        // Перебираємо всі кімнати
        for (Map.Entry<String, Set<String>> entry : roomParticipants.entrySet()) {
            Set<String> participants = entry.getValue();
            if (participants.remove(email)) {
                participants.add(uuid);
                log.info("🔄 Замінено email {} на UUID {} в кімнаті {}", email, uuid, entry.getKey());
            }
        }

        // Оновлюємо зворотний індекс
        Set<String> rooms = userRooms.remove(email);
        if (rooms != null) {
            userRooms.put(uuid, rooms);
            log.debug("🔄 Оновлено індекс: {} -> {}", email, uuid);
        }
    }
}