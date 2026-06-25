package media.toloka.rfa.media.messanger.webrtc.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Оптимізована версія сервісу для керування WebRTC трансляціями.
 *
 * Ключові відмінності від v1:
 * - Використовує JanusRabbitClientOptimized з batch-операціями
 * - Admin API для отримання списку кімнат (без створення сесій)
 * - Оптимізована синхронізація стану БД ↔ Janus
 * - Менше запитів до Janus = менше таймаутів
 */
@Service
public class RadioLiveServiceV2 {

    private static final Logger log = LoggerFactory.getLogger(RadioLiveServiceV2.class);

    private final JanusRabbitClientOptimized janusClient;
    private final PortAllocationService portService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    private ClientService clientService;

    @Autowired
    private MessangerService messangerService;

    @Autowired
    private StationService stationService;

    public RadioLiveServiceV2(
            JanusRabbitClientOptimized janusClient,
            PortAllocationService portService,
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper) {
        this.janusClient = janusClient;
        this.portService = portService;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    // ============================================================
    // УПРАВЛІННЯ ТРАНЛЯЦІЯМИ
    // ============================================================

    /**
     * Запуск трансляції радіостанції.
     */
    @Transactional
    public JanusLiveStream startBroadcast(String stationId) {
        return portService.getStreamByStation(stationId).orElseGet(() -> {
            log.info("Запуск трансляції для станції: {}", stationId);

            String sourceStreamUrl = "http://icecast.rfa:8000/ukraine";
            Integer freePort = portService.findNextFreePort();
            log.info("Виділено порт: {}", freePort);

            // Створюємо кімнату в Janus
            Long janusRoomId = Long.parseLong(stationId.replaceAll("[^0-9]", ""));
            janusClient.createAudioRoom(janusRoomId, "Ефір: " + stationId, freePort);
            log.info("Створено кімнату в Janus: {}", janusRoomId);

            // Запускаємо FFmpeg
            sendFfmpegCommand("start", stationId, sourceStreamUrl, "rtp://janus.rfa:" + freePort);

            return portService.saveActiveStream(stationId, janusRoomId, freePort, 0);
        });
    }

    /**
     * Зупинка трансляції.
     */
    @Transactional
    public void stopBroadcast(String stationId) {
        portService.getStreamByStation(stationId).ifPresent(stream -> {
            log.info("Зупинка трансляції: {}", stationId);
            sendFfmpegCommand("stop", stationId, null, null);
            portService.deleteStream(stationId);
        });
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
        janusClient.registerToken(token);

        return token;
    }

    /**
     * Перевіряє, чи токен користувача зареєстрований в Janus.
     * Оптимізовано: використовує batch-запит до Admin API.
     */
    @Transactional(readOnly = true)
    public Boolean checkUserTokenInJanus() {
        Users user = clientService.GetCurrentUser();
        if (user == null) return false;

        Clientdetail cd = clientService.GetClientDetailByUser(user);
        if (cd == null) return false;

        String userToken = cd.getUuid();
        boolean isRegistered = janusClient.isTokenRegistered(userToken);

        log.info("Токен користувача {} {} в Janus",
                userToken, isRegistered ? "знайдено" : "НЕ знайдено");

        return isRegistered;
    }

    // ============================================================
    // ОТРИМАННЯ СПИСКУ КІМНАТ (ОПТИМІЗОВАНО)
    // ============================================================

    /**
     * Отримує список активних кімнат з учасниками та статусом трансляції.
     *
     * ОПТИМІЗАЦІЯ v2:
     * - Використовує Admin API (без createSession/destroySession)
     * - 1 запит на list + N запитів на listparticipants
     * - Замість 4+N запитів у v1
     */
    @Transactional
    public List<StationDto> getActiveRoomsInfo() {
        log.debug("Отримання списку активних кімнат (оптимізована версія)");

        // 1. Джерело правди — БД
        List<StationDto> activeStations = stationService.getActiveStations();
        Set<Long> validRoomIds = activeStations.stream()
                .map(StationDto::getId)
                .collect(Collectors.toSet());

        // 2. Отримуємо кімнати з Janus (ОПТИМІЗОВАНО через Admin API)
        List<Map<String, Object>> janusRooms;
        try {
            janusRooms = janusClient.getAllRoomsWithParticipants();
            log.debug("Отримано {} кімнат з Janus", janusRooms.size());
        } catch (Exception e) {
            log.error("Помилка отримання даних з Janus, повертаємо дані з БД", e);
            return activeStations;
        }

        // 3. Перетворюємо в Map для швидкого пошуку
        Map<Long, Map<String, Object>> janusRoomsMap = janusRooms.stream()
                .filter(r -> r.get("room") != null)
                .collect(Collectors.toMap(
                        r -> ((Number) r.get("room")).longValue(),
                        r -> r,
                        (a, b) -> a
                ));

        // 4. Синхронізація: видаляємо зайві кімнати з Janus
        for (Long janusRoomId : janusRoomsMap.keySet()) {
            if (!validRoomIds.contains(janusRoomId)) {
                log.warn("Зайва кімната в Janus: {}. Видаляємо...", janusRoomId);

                Map<String, Object> orphanRoom = janusRoomsMap.get(janusRoomId);
                List<?> participants = (List<?>) orphanRoom.get("participants");

                // Евакуація користувачів
                if (participants != null && !participants.isEmpty()) {
                    log.info("Евакуація {} користувачів з кімнати {}",
                            participants.size(), janusRoomId);
                    try {
                        messangerService.moveUsersToMainChatRoom(janusRoomId);
                    } catch (Exception e) {
                        log.error("Помилка евакуації користувачів", e);
                    }
                }

                // Видаляємо кімнату
                try {
                    janusClient.destroyAudioRoom(janusRoomId);
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
                // Кімната є в Janus
                station.setStationstate(true);
                List<Object> participants = (List<Object>) jRoomData.get("participants");
                station.setParticipants(participants != null ? participants : new ArrayList<>());

            } else {
                // Кімнати немає в Janus — створюємо на льоту
                log.info("Кімната {} відсутня в Janus. Створюємо...", roomId);

                try {
                    // Перевіряємо чи є порт в БД
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

                    // Створюємо кімнату в Janus
                    janusClient.createAudioRoom(roomId, station.getName(), currentRtpPort);

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
     * Оптимізовано: використовує batch-запити для перевірки інших кімнат.
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
            // 1. Отримуємо список всіх активних кімнат (ОДИН запит)
            List<Long> activeJanusRooms = janusClient.getActiveRoomsIds();

            if (activeJanusRooms != null) {
                // 2. Перевіряємо інші кімнати та видаляємо користувача
                for (Long activeRoomId : activeJanusRooms) {
                    if (!activeRoomId.equals(roomID)) {
                        boolean isUserInRoom = janusClient.isUserInRoom(activeRoomId, userUUID);

                        if (isUserInRoom) {
                            log.info("Користувач знайдений в кімнаті {}. Видаляємо...", activeRoomId);
                            janusClient.kickUser(activeRoomId, userUUID);
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
    // ДОПОМІЖНІ МЕТОДИ
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

    /**
     * Відправляє команду FFmpeg агенту через RabbitMQ.
     */
    private void sendFfmpegCommand(String command, String stationId, String streamUrl, String rtpUrl) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("command", command);
            message.put("stationId", stationId);

            if ("start".equals(command)) {
                message.put("streamUrl", streamUrl);
                message.put("rtpUrl", rtpUrl);
            }

            String jsonPayload = objectMapper.writeValueAsString(message);
            log.info("Відправка команди FFmpeg: {}", jsonPayload);
            rabbitTemplate.convertAndSend("ffmpeg-agent-commands", jsonPayload);

        } catch (Exception e) {
            throw new RuntimeException("Помилка відправки команди FFmpeg: " + e.getMessage());
        }
    }
}