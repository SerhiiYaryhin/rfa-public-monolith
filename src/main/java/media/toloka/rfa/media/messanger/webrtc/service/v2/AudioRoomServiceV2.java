package media.toloka.rfa.media.messanger.webrtc.service.v2;

import media.toloka.rfa.media.messanger.service.MessangerService;
import media.toloka.rfa.media.messanger.webrtc.model.JanusLiveStream;
import media.toloka.rfa.media.messanger.webrtc.model.dto.StationDto;
import media.toloka.rfa.media.messanger.webrtc.service.PortAllocationService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.station.service.StationService;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервіс для керування аудіокімнатами та підключеннями користувачів.
 *
 * Відповідальність:
 * - Отримання списку активних кімнат з учасниками
 * - Підключення/відключення користувачів до кімнат
 * - Управління токенами авторизації в Janus
 * - Синхронізація стану між БД та Janus
 */
@Service
public class AudioRoomServiceV2 {

    private static final Logger log = LoggerFactory.getLogger(AudioRoomServiceV2.class);

    private final JanusRabbitClientOptimized janusClient;
    private final PortAllocationService portService;
    private final ClientService clientService;
    private final MessangerService messangerService;
    private final StationService stationService;

    public AudioRoomServiceV2(
            JanusRabbitClientOptimized janusClient,
            PortAllocationService portService,
            ClientService clientService,
            MessangerService messangerService,
            StationService stationService) {
        this.janusClient = janusClient;
        this.portService = portService;
        this.clientService = clientService;
        this.messangerService = messangerService;
        this.stationService = stationService;
    }

    // ============================================================
    // УПРАВЛІННЯ ТОКЕНАМИ
    // ============================================================

    /**
     * Отримує токен користувача (UUID) та реєструє його в Janus.
     */
    public String generateAndRegisterToken() {
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) {
            log.warn("Користувач не авторизований");
            return null;
        }

        String token = cd.getUuid();
        log.info("Реєстрація токена користувача: {}", token);
        janusClient.registerUserToken(token);

        return token;
    }

    /**
     * Перевіряє, чи токен користувача зареєстрований в Janus.
     */
    @Transactional(readOnly = true)
    public Boolean checkUserTokenInJanus() {
        Users user = clientService.GetCurrentUser();
        if (user == null) return false;

        Clientdetail cd = clientService.GetClientDetailByUser(user);
        if (cd == null) return false;

        String userToken = cd.getUuid();
        boolean isRegistered = janusClient.checkUserTokenInJanus(userToken);

        log.info("Токен користувача {} {} в Janus",
                userToken, isRegistered ? "знайдено" : "НЕ знайдено");

        return isRegistered;
    }

    // ============================================================
    // ОТРИМАННЯ СПИСКУ КІМНАТ
    // ============================================================

    /**
     * Отримує список активних кімнат з учасниками та статусом трансляції.
     */
    @Transactional
    public List<StationDto> getActiveRoomsInfo() {
        log.debug("Отримання списку активних кімнат");

        // 1. Джерело правди — БД
        List<StationDto> activeStations = stationService.getActiveStations();
        Set<Long> validRoomIds = activeStations.stream()
                .map(StationDto::getId)
                .collect(Collectors.toSet());

        // 2. Отримуємо кімнати з Janus
        List<Map<String, Object>> janusRooms;
        try {
            janusRooms = janusClient.getJanusRoomsAndParticipants();
            log.debug("Отримано {} кімнат з Janus", janusRooms.size());
        } catch (Exception e) {
            log.error("Помилка отримання даних з Janus, повертаємо дані з БД", e);
            return activeStations;
        }

        // 3. Перетворюємо в Map для швидкого пошуку
        Map<Long, Map<String, Object>> janusRoomsMap = new HashMap<>();
        for (Map<String, Object> jRoom : janusRooms) {
            Number roomIdNum = (Number) jRoom.get("room");
            if (roomIdNum != null) {
                janusRoomsMap.put(roomIdNum.longValue(), jRoom);
            }
        }

        // 4. Синхронізація: видаляємо зайві кімнати з Janus
        for (Long janusRoomId : janusRoomsMap.keySet()) {
            if (!validRoomIds.contains(janusRoomId)) {
                log.warn("Зайва кімната в Janus: {}. Видаляємо...", janusRoomId);

                Map<String, Object> orphanRoom = janusRoomsMap.get(janusRoomId);
                List<?> participants = (List<?>) orphanRoom.get("participants");

                if (participants != null && !participants.isEmpty()) {
                    log.info("Евакуація {} користувачів з кімнати {}",
                            participants.size(), janusRoomId);
                    try {
                        messangerService.moveUsersToMainChatRoom(janusRoomId);
                    } catch (Exception e) {
                        log.error("Помилка евакуації користувачів", e);
                    }
                }

                try {
                    janusClient.destroyAudioBridgeRoom(janusRoomId);
                    log.info("Кімнату {} видалено з Janus", janusRoomId);
                } catch (Exception e) {
                    log.error("Помилка видалення кімнати {}", janusRoomId, e);
                }
            }
        }

        // 5. Збагачуємо дані станцій інформацією з Janus
        for (StationDto station : activeStations) {
            Long roomId = station.getId();
            String stationIdStr = String.valueOf(roomId);

            Map<String, Object> jRoomData = janusRoomsMap.get(roomId);

            if (jRoomData != null) {
                station.setStationstate(true);
                List<Object> participants = (List<Object>) jRoomData.get("participants");
                station.setParticipants(participants != null ? participants : new ArrayList<>());

            } else {
                log.info("Кімната {} відсутня в Janus. Створюємо...", roomId);

                try {
                    Optional<JanusLiveStream> existingStream = portService.getStreamByStation(stationIdStr);
                    int currentRtpPort;

                    if (existingStream.isPresent()) {
                        currentRtpPort = existingStream.get().getRtpPort();
                        log.debug("Використовуємо існуючий порт: {}", currentRtpPort);
                    } else {
                        currentRtpPort = portService.findNextFreePort();
                        log.debug("Виділено новий порт: {}", currentRtpPort);
                        portService.saveActiveStream(stationIdStr, roomId, currentRtpPort, 0);
                    }

                    janusClient.createAudioBridgeRoom(roomId, station.getName(), currentRtpPort);

                    station.setStationstate(true);
                    station.setParticipants(new ArrayList<>());

                } catch (Exception e) {
                    log.error("Помилка створення кімнати {}", roomId, e);
                    station.setStationstate(false);
                }
            }
        }

        return activeStations;
    }

    // ============================================================
    // ПІДКЛЮЧЕННЯ КОРИСТУВАЧА ДО КІМНАТИ
    // ============================================================

    /**
     * Підключає користувача до аудіокімнати.
     */
    public Map<String, Object> connectClientToAudioRoom(Long roomID) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            log.warn("Користувач не залогінений");
            return Map.of("success", false, "error", "Користувач не залогінений");
        }

        Clientdetail cd = clientService.GetClientDetailByUser(user);
        String userName = cd.getCustname() + " " + cd.getCustsurname();
        String userUUID = cd.getUuid();

        log.info("Підключення користувача {} (UUID: {}) до кімнати {}",
                userName, userUUID, roomID);

        try {
            // 1. Отримуємо список всіх активних кімнат
            List<Long> activeJanusRooms = janusClient.getActiveRoomsIds();

            if (activeJanusRooms != null) {
                // 2. Перевіряємо інші кімнати та видаляємо користувача
                for (Long activeRoomId : activeJanusRooms) {
                    if (!activeRoomId.equals(roomID)) {
                        boolean isUserInRoom = janusClient.isUserInAudioRoom(activeRoomId, userUUID);

                        if (isUserInRoom) {
                            log.info("Користувач знайдений в кімнаті {}. Видаляємо...", activeRoomId);
                            janusClient.kickUserFromAudioBridgeRoom(activeRoomId, userUUID);
                        }
                    }
                }
            }

            // 3. Формуємо відповідь
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("roomid", roomID);
            result.put("token", userUUID);
            result.put("display", userName);
            result.put("userId", cd.getId());

            return result;

        } catch (Exception e) {
            log.error("Помилка підключення до кімнати {}", roomID, e);
            return Map.of(
                    "success", false,
                    "error", "Помилка підключення: " + e.getMessage()
            );
        }
    }

    // ============================================================
    // ДІАГНОСТИКА
    // ============================================================

    /**
     * Перевірка зв'язку з Janus.
     */
    public boolean pingJanus() {
        return janusClient.pingJanus();
    }

    /**
     * Отримує API secret (НЕ передавати клієнту!).
     */
    public String getApiSecret() {
        return janusClient.getApiSecret();
    }
}