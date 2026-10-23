package media.toloka.rfa.media.messanger.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

    /**
     * @Lazy необхідний для розриву циклічної залежності з WebSocket конфігурацією.
     */
    @Lazy
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /** Окремий топік для сповіщення панелі модератора про зміни в складі учасників. */
    private static final String EFIR_PRESENCE_TOPIC = "/topic/efir.room.presence";

    public void addUserToRoom(String userIdentifier, String roomUuid) {
        if (userIdentifier == null || roomUuid == null) return;

        boolean changed = roomParticipants.computeIfAbsent(roomUuid, k -> ConcurrentHashMap.newKeySet())
                .add(userIdentifier);
        userRooms.computeIfAbsent(userIdentifier, k -> ConcurrentHashMap.newKeySet())
                .add(roomUuid);

        if (changed) {
            log.debug("✅ Додано {} в кімнату {}", userIdentifier, roomUuid);
            notifyModerators();
        }
    }

    public void removeUserFromRoom(String userIdentifier, String roomUuid) {
        if (userIdentifier == null || roomUuid == null) return;

        Set<String> participants = roomParticipants.get(roomUuid);
        boolean changed = false;
        if (participants != null) {
            changed = participants.remove(userIdentifier);
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

        if (changed) {
            log.debug("❌ Видалено {} з кімнати {}", userIdentifier, roomUuid);
            notifyModerators();
        }
    }

    public void removeUserFromAllRooms(String userIdentifier) {
        if (userIdentifier == null) return;

        Set<String> rooms = userRooms.remove(userIdentifier);
        if (rooms != null && !rooms.isEmpty()) {
            for (String roomUuid : rooms) {
                Set<String> participants = roomParticipants.get(roomUuid);
                if (participants != null) {
                    participants.remove(userIdentifier);
                    if (participants.isEmpty()) {
                        roomParticipants.remove(roomUuid);
                    }
                }
            }
            log.debug("👋 Видалено {} з усіх кімнат", userIdentifier);
            notifyModerators();
        }
    }

    /**
     * Переміщує користувача в нову кімнату, автоматично видаляючи його з усіх інших.
     * Вирішує проблему, коли STOMP UNSUBSCRIBE приходить без destination.
     */
    public void moveUserToRoom(String userIdentifier, String newRoomUuid) {
        if (userIdentifier == null || newRoomUuid == null) return;

        Set<String> currentRooms = userRooms.get(userIdentifier);
        boolean changed = false;

        if (currentRooms != null && !currentRooms.isEmpty()) {
            Set<String> roomsCopy = ConcurrentHashMap.newKeySet();
            roomsCopy.addAll(currentRooms);

            for (String roomUuid : roomsCopy) {
                if (!roomUuid.equals(newRoomUuid)) {
                    Set<String> participants = roomParticipants.get(roomUuid);
                    if (participants != null && participants.remove(userIdentifier)) {
                        if (participants.isEmpty()) {
                            roomParticipants.remove(roomUuid);
                        }
                        log.info("🔄 Користувач {} переміщений з кімнати {} в {}",
                                userIdentifier, roomUuid, newRoomUuid);
                        changed = true;
                    }
                    currentRooms.remove(roomUuid);
                }
            }
        }

        // Додаємо користувача в нову кімнату
        boolean added = roomParticipants.computeIfAbsent(newRoomUuid, k -> ConcurrentHashMap.newKeySet())
                .add(userIdentifier);
        userRooms.computeIfAbsent(userIdentifier, k -> ConcurrentHashMap.newKeySet())
                .add(newRoomUuid);

        if (added || changed) {
            notifyModerators();
        }
    }

    public Set<String> getRoomParticipants(String roomUuid) {
        return roomParticipants.getOrDefault(roomUuid, Collections.emptySet());
    }

    public Set<String> getUserRooms(String userIdentifier) {
        return userRooms.getOrDefault(userIdentifier, Collections.emptySet());
    }

    public void replaceEmailWithUuid(String email, String uuid) {
        if (email == null || uuid == null || email.equals(uuid)) {
            return;
        }

        boolean changed = false;
        for (Map.Entry<String, Set<String>> entry : roomParticipants.entrySet()) {
            Set<String> participants = entry.getValue();
            if (participants.remove(email)) {
                participants.add(uuid);
                log.info("🔄 Замінено email {} на UUID {} в кімнаті {}", email, uuid, entry.getKey());
                changed = true;
            }
        }

        Set<String> rooms = userRooms.remove(email);
        if (rooms != null) {
            userRooms.put(uuid, rooms);
            log.debug("🔄 Оновлено індекс: {} -> {}", email, uuid);
            changed = true;
        }

        if (changed) {
            notifyModerators();
        }
    }

    /**
     * ✅ Сповіщаємо всіх клієнтів (включно з панеллю модератора) про зміну в складі учасників.
     * Панель модератора підписана на цей топік і автоматично оновлює список.
     */
    private void notifyModerators() {
        if (messagingTemplate != null) {
            try {
                messagingTemplate.convertAndSend(EFIR_PRESENCE_TOPIC, "REFRESH");
                log.debug("📣 Опубліковано сповіщення про оновлення учасників");
            } catch (Exception e) {
                log.warn("⚠️ Не вдалося опублікувати сповіщення: {}", e.getMessage());
            }
        }
    }
}