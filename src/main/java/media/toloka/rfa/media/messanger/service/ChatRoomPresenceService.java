package media.toloka.rfa.media.messanger.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ChatRoomPresenceService {

    private final Map<String, Set<String>> roomParticipants = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userRooms = new ConcurrentHashMap<>();
    private final Map<String, Boolean> userMutedState = new ConcurrentHashMap<>();

    @Lazy
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final String EFIR_PRESENCE_TOPIC = "/topic/efir.room.presence";

    public void addUserToRoom(String userIdentifier, String roomUuid) {
        if (userIdentifier == null || roomUuid == null) return;
        boolean changed = roomParticipants.computeIfAbsent(roomUuid, k -> ConcurrentHashMap.newKeySet())
                .add(userIdentifier);
        userRooms.computeIfAbsent(userIdentifier, k -> ConcurrentHashMap.newKeySet()).add(roomUuid);
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
            if (participants.isEmpty()) roomParticipants.remove(roomUuid);
        }
        Set<String> rooms = userRooms.get(userIdentifier);
        if (rooms != null) {
            rooms.remove(roomUuid);
            if (rooms.isEmpty()) userRooms.remove(userIdentifier);
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
                    if (participants.isEmpty()) roomParticipants.remove(roomUuid);
                }
            }
            log.debug("👋 Видалено {} з усіх кімнат", userIdentifier);
            notifyModerators();
        }
    }

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
                        if (participants.isEmpty()) roomParticipants.remove(roomUuid);
                        log.info("🔄 Користувач {} переміщений з кімнати {} в {}",
                                userIdentifier, roomUuid, newRoomUuid);
                        changed = true;
                    }
                    currentRooms.remove(roomUuid);
                }
            }
        }
        boolean added = roomParticipants.computeIfAbsent(newRoomUuid, k -> ConcurrentHashMap.newKeySet())
                .add(userIdentifier);
        userRooms.computeIfAbsent(userIdentifier, k -> ConcurrentHashMap.newKeySet()).add(newRoomUuid);
        if (added || changed) notifyModerators();
    }

    public Set<String> getRoomParticipants(String roomUuid) {
        return roomParticipants.getOrDefault(roomUuid, Collections.emptySet());
    }

    public Set<String> getUserRooms(String userIdentifier) {
        return userRooms.getOrDefault(userIdentifier, Collections.emptySet());
    }

    public void setMuted(String userUuid, boolean muted) {
        if (userUuid == null) return;
        Boolean previous = userMutedState.put(userUuid, muted);
        if (previous == null || previous != muted) {
            log.info("🎙️ Користувач {} тепер {}", userUuid, muted ? "MUTED 🔇" : "UNMUTED 🔊");
            notifyModerators();
        }
    }

    public boolean isMuted(String userUuid) {
        return userMutedState.getOrDefault(userUuid, Boolean.FALSE);
    }

    public void muteAllInRoom(String roomUuid) {
        Set<String> participants = roomParticipants.get(roomUuid);
        if (participants == null || participants.isEmpty()) return;
        for (String userUuid : participants) userMutedState.put(userUuid, true);
        log.info("🔇 Mute All у кімнаті {} — {} учасників", roomUuid, participants.size());
        notifyModerators();
    }

    public void unmuteAllInRoom(String roomUuid) {
        Set<String> participants = roomParticipants.get(roomUuid);
        if (participants == null || participants.isEmpty()) return;
        for (String userUuid : participants) userMutedState.put(userUuid, false);
        log.info("🔊 Unmute All у кімнаті {} — {} учасників", roomUuid, participants.size());
        notifyModerators();
    }

    public void replaceEmailWithUuid(String email, String uuid) {
        if (email == null || uuid == null || email.equals(uuid)) return;
        boolean changed = false;
        for (Map.Entry<String, Set<String>> entry : roomParticipants.entrySet()) {
            Set<String> participants = entry.getValue();
            if (participants.remove(email)) {
                participants.add(uuid);
                log.info("🔄 Замінено email {} на UUID {} в кімнаті {}", email, uuid, entry.getKey());
                changed = true;
            }
        }
        Boolean mutedState = userMutedState.remove(email);
        if (mutedState != null) {
            userMutedState.put(uuid, mutedState);
            changed = true;
        }
        Set<String> rooms = userRooms.remove(email);
        if (rooms != null) {
            userRooms.put(uuid, rooms);
            changed = true;
        }
        if (changed) notifyModerators();
    }

    private void notifyModerators() {
        if (messagingTemplate != null) {
            try {
                messagingTemplate.convertAndSend(EFIR_PRESENCE_TOPIC, "REFRESH");
            } catch (Exception e) {
                log.warn("⚠️ Не вдалося опублікувати сповіщення: {}", e.getMessage());
            }
        }
    }
}