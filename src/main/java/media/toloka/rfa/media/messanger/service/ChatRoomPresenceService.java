package media.toloka.rfa.media.messanger.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🎯 Сервіс для відстеження учасників конкретних кімнат чату.
 *
 * Відстежує STOMP підписки на /topic/{roomUuid} та зберігає в пам'яті.
 * Кожен вузол кластера має власну копію цього сервісу, але оскільки
 * підписки йдуть через RabbitMQ STOMP Relay, інтерцептор спрацьовує
 * на тому вузлі, до якого підключений клієнт.
 *
 * ️ Обмеження: у кластері кожен вузол бачить тільки тих користувачів,
 * які підключені до нього. Для повного списку потрібен Redis або
 * централізоване сховище (реалізується окремим етапом).
 */
@Slf4j
@Service
public class ChatRoomPresenceService {

    /** roomUuid → Set<userUuid> */
    private final ConcurrentHashMap<String, Set<String>> roomParticipants = new ConcurrentHashMap<>();

    /** userUuid → Set<roomUuid> (для швидкого видалення при відключенні) */
    private final ConcurrentHashMap<String, Set<String>> userRooms = new ConcurrentHashMap<>();

    /**
     * Додати користувача в кімнату (при STOMP SUBSCRIBE на /topic/{roomUuid}).
     */
    public void addUserToRoom(String userUuid, String roomUuid) {
        if (userUuid == null || roomUuid == null) return;

        roomParticipants.computeIfAbsent(roomUuid, k -> ConcurrentHashMap.newKeySet()).add(userUuid);
        userRooms.computeIfAbsent(userUuid, k -> ConcurrentHashMap.newKeySet()).add(roomUuid);

        log.debug("✅ Користувач {} доданий в кімнату {}", userUuid, roomUuid);
    }

    /**
     * Видалити користувача з кімнати (при STOMP UNSUBSCRIBE).
     */
    public void removeUserFromRoom(String userUuid, String roomUuid) {
        if (userUuid == null || roomUuid == null) return;

        Set<String> participants = roomParticipants.get(roomUuid);
        if (participants != null) {
            participants.remove(userUuid);
            if (participants.isEmpty()) {
                roomParticipants.remove(roomUuid);
            }
        }

        Set<String> rooms = userRooms.get(userUuid);
        if (rooms != null) {
            rooms.remove(roomUuid);
            if (rooms.isEmpty()) {
                userRooms.remove(userUuid);
            }
        }

        log.debug("❌ Користувач {} видалений з кімнати {}", userUuid, roomUuid);
    }

    /**
     * Видалити користувача з ВСІХ кімнат (при відключенні WebSocket).
     */
    public void removeUserFromAllRooms(String userUuid) {
        if (userUuid == null) return;

        Set<String> rooms = userRooms.remove(userUuid);
        if (rooms != null) {
            for (String roomUuid : rooms) {
                Set<String> participants = roomParticipants.get(roomUuid);
                if (participants != null) {
                    participants.remove(userUuid);
                    if (participants.isEmpty()) {
                        roomParticipants.remove(roomUuid);
                    }
                }
            }
        }

        log.debug(" Користувач {} видалений з усіх кімнат", userUuid);
    }

    /**
     * Отримати список учасників кімнати.
     */
    public Set<String> getRoomParticipants(String roomUuid) {
        return roomParticipants.getOrDefault(roomUuid, Collections.emptySet());
    }

    /**
     * Отримати список кімнат користувача.
     */
    public Set<String> getUserRooms(String userUuid) {
        return userRooms.getOrDefault(userUuid, Collections.emptySet());
    }

    /**
     * Перевірити, чи користувач у кімнаті.
     */
    public boolean isUserInRoom(String userUuid, String roomUuid) {
        Set<String> participants = roomParticipants.get(roomUuid);
        return participants != null && participants.contains(userUuid);
    }

    /**
     * Отримати кількість учасників кімнати (для лічильників).
     */
    public int getRoomParticipantsCount(String roomUuid) {
        Set<String> participants = roomParticipants.get(roomUuid);
        return participants == null ? 0 : participants.size();
    }
}