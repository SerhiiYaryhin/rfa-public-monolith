package media.toloka.rfa.media.webrtc.service.v2;

import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 👤 Сервіс управління підключенням користувачів до аудіокімнат.
 */
@Service
public class UserConnectionService {

    private static final Logger log = LoggerFactory.getLogger(UserConnectionService.class);

    // ✅ Використовуємо прості імена класів, оскільки вони в тому ж пакеті
    private final JanusRoomService roomService;
    private final JanusParticipantService participantService;
    private final JanusAdminService adminService;
    private final ClientService clientService;

    public UserConnectionService(
            JanusRoomService roomService,
            JanusParticipantService participantService,
            JanusAdminService adminService,
            ClientService clientService) {
        this.roomService = roomService;
        this.participantService = participantService;
        this.adminService = adminService;
        this.clientService = clientService;
    }

    public String generateAndRegisterToken() {
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) {
            log.warn("Користувач не авторизований");
            return null;
        }

        String token = cd.getUuid();
        log.info("Реєстрація токена користувача: {}", token);
        adminService.registerUserToken(token);
        return token;
    }

    @Transactional(readOnly = true)
    public Boolean checkUserTokenInJanus() {
        Users user = clientService.GetCurrentUser();
        if (user == null) return false;

        Clientdetail cd = clientService.GetClientDetailByUser(user);
        if (cd == null) return false;

        String userToken = cd.getUuid();
        boolean isRegistered = adminService.checkUserTokenInJanus(userToken);

        log.info("Токен користувача {} {} в Janus", userToken, isRegistered ? "знайдено" : "НЕ знайдено");
        return isRegistered;
    }

    public Map<String, Object> connectClientToAudioRoom(Long roomID) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            log.warn("Користувач не залогінений");
            return Map.of("success", false, "error", "Користувач не залогінений");
        }

        Clientdetail cd = clientService.GetClientDetailByUser(user);
        String userName = cd.getCustname() + " " + cd.getCustsurname();
        String userUUID = cd.getUuid();

        log.info("Підключення користувача {} (UUID: {}) до кімнати {}", userName, userUUID, roomID);

        try {
            evacuateUserFromOtherRooms(userUUID, roomID);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("roomid", roomID);
            result.put("token", userUUID);
            result.put("display", userName);
            result.put("userId", cd.getId());

            return result;
        } catch (Exception e) {
            log.error("Помилка підключення до кімнати {}", roomID, e);
            return Map.of("success", false, "error", "Помилка підключення: " + e.getMessage());
        }
    }

    private void evacuateUserFromOtherRooms(String userUUID, Long targetRoomId) {
        List<Long> activeJanusRooms = roomService.getActiveRoomsIds();

        if (activeJanusRooms != null) {
            for (Long activeRoomId : activeJanusRooms) {
                if (!activeRoomId.equals(targetRoomId)) {
                    boolean isUserInRoom = roomService.isUserInAudioRoom(activeRoomId, userUUID);
                    if (isUserInRoom) {
                        log.info("Користувач знайдений в кімнаті {}. Видаляємо...", activeRoomId);
                        participantService.kickUserFromAudioBridgeRoom(activeRoomId, userUUID);
                    }
                }
            }
        }
    }
}