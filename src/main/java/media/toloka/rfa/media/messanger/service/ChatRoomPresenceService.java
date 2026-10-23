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
                Set<String> participants = roomParticipants.get(roomUuid);
                if (participants != null) {
                    participants.remove(userIdentifier);
                    if (participants.isEmpty()) {
                        roomParticipants.remove(roomUuid);
                    }
                }
            }
        }
        log.debug("👋 Видалено {} з усіх кімнат", userIdentifier);
    }

    /**
     * ✅ НОВИЙ МЕТОД: Переміщує користувача в нову кімнату,
     * автоматично видаляючи його з усіх інших кімнат.
     *
     * Це вирішує проблему, коли STOMP UNSUBSCRIBE приходить без destination.
     * Оскільки користувач в чаті може бути тільки в одній кімнаті одночасно,
     * ми просто "переносимо" його при кожному новому SUBSCRIBE.
     */
    public void moveUserToRoom(String userIdentifier, String newRoomUuid) {
        if (userIdentifier == null || newRoomUuid == null) return;

        // 1. Видаляємо користувача з усіх поточних кімнат
        Set<String> currentRooms = userRooms.get(userIdentifier);
        if (currentRooms != null) {
            // Створюємо копію, щоб уникнути ConcurrentModificationException
            Set<String> roomsCopy = ConcurrentHashMap.newKeySet();
            roomsCopy.addAll(currentRooms);

            for (String roomUuid : roomsCopy) {
                if (!roomUuid.equals(newRoomUuid)) {
                    removeUserFromRoom(userIdentifier, roomUuid);
                    log.info("🔄 Користувач {} переміщений з кімнати {} в {}",
                            userIdentifier, roomUuid, newRoomUuid);
                }
            }
        }

        // 2. Додаємо користувача в нову кімнату (якщо його там ще немає)
        addUserToRoom(userIdentifier, newRoomUuid);
    }

    public Set<String> getRoomParticipants(String roomUuid) {
        return roomParticipants.getOrDefault(roomUuid, Collections.emptySet());
    }

    public Set<String> getUserRooms(String userIdentifier) {
        return userRooms.getOrDefault(userIdentifier, Collections.emptySet());
    }

    /**
     * Замінює всі входження email на UUID у всіх кімнатах.
     * Викликається з /app/hello, коли стає відомий справжній UUID користувача.
     */
    public void replaceEmailWithUuid(String email, String uuid) {
        if (email == null || uuid == null || email.equals(uuid)) {
            return;
        }

        for (Map.Entry<String, Set<String>> entry : roomParticipants.entrySet()) {
            Set<String> participants = entry.getValue();
            if (participants.remove(email)) {
                participants.add(uuid);
                log.info("🔄 Замінено email {} на UUID {} в кімнаті {}", email, uuid, entry.getKey());
            }
        }

        Set<String> rooms = userRooms.remove(email);
        if (rooms != null) {
            userRooms.put(uuid, rooms);
            log.debug("🔄 Оновлено індекс: {} -> {}", email, uuid);
        }
    }
}