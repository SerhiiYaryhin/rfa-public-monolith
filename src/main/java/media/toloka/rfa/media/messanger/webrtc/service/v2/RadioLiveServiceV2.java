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

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Високорівневий сервіс для керування прямими ефірами через WebRTC (v2).
 *
 * Ключові відмінності від v1:
 * - Використовує JanusRabbitClientOptimized
 * - Автоматичний запуск трансляцій при старті сервера (@PostConstruct)
 * - createAudioBridgeRoom() передає RTP параметри для прийому зовнішнього потоку
 * - startBroadcast() використовує Long для roomId (збігається з ID станції з БД)
 */
@Service
public class RadioLiveServiceV2 {

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

    private static final Logger log = LoggerFactory.getLogger(RadioLiveServiceV2.class);

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
    // ІНІЦІАЛІЗАЦІЯ ПРИ СТАРТІ СЕРВЕРА
    // ============================================================

    /**
     * Автоматичний запуск трансляцій при старті сервера.
     * Для кожної активної станції перевіряє чи запущено трансляцію,
     * якщо ні — запускає її.
     */
    @PostConstruct
    @Transactional
    public void initializeBroadcasts() {
        log.info("🚀 Ініціалізація трансляцій при старті сервера...");

        try {
            // 1. Отримуємо всі станції з онлайн-статусом
            List<StationDto> onlineStations = stationService.getActiveStations();

            if (onlineStations.isEmpty()) {
                log.info("Немає активних станцій для запуску трансляцій");
                return;
            }

            log.info("Знайдено {} активних станцій", onlineStations.size());

            // 2. Для кожної станції перевіряємо чи запущено трансляцію
            for (StationDto station : onlineStations) {
                String stationId = String.valueOf(station.getId());

                Optional<JanusLiveStream> existingStream = portService.getStreamByStation(stationId);

                if (existingStream.isEmpty()) {
                    log.info("Станція {} не має активної трансляції. Запускаємо...", station.getName());

                    try {
                        startBroadcast(stationId);
                        log.info("✅ Трансляцію для станції {} запущено", station.getName());
                    } catch (Exception e) {
                        log.error("❌ Помилка запуску трансляції для станції {}: {}",
                                station.getName(), e.getMessage(), e);
                    }
                } else {
                    log.info("Станція {} вже має активну трансляцію на порту {}",
                            station.getName(), existingStream.get().getRtpPort());
                }
            }

            log.info("✅ Ініціалізацію трансляцій завершено");

        } catch (Exception e) {
            log.error("❌ Помилка ініціалізації трансляцій", e);
        }
    }

    // ============================================================
    // УПРАВЛІННЯ ТРАНЛЯЦІЯМИ
    // ============================================================

    /**
     * Запуск трансляції радіостанції в WebRTC ефір.
     *
     * @param stationId ID станції (наприклад, "352" або "802")
     * @return Дані про створений стрім
     */
    @Transactional
    public JanusLiveStream startBroadcast(String stationId) {
        return portService.getStreamByStation(stationId).orElseGet(() -> {
            log.info("Запуск трансляції для станції: {}", stationId);

            String sourceStreamUrl = "http://icecast.rfa:8000/ukraine";
            Integer freePort = portService.findNextFreePort();
            log.info("Виділено порт для трансляції: {}", freePort);

            Long janusRoomId = Long.parseLong(stationId.replaceAll("[^0-9]", ""));

            // 1. Створюємо кімнату (БЕЗ rtp_stream параметрів)
            janusClient.createAudioBridgeRoom(janusRoomId, "Ефір: " + stationId, freePort);
            log.info("Кімнату {} створено", janusRoomId);

            // 2. ✅ НОВЕ: Створюємо RTP participant для прийому потоку
            Long rtpParticipantId = janusClient.createRtpParticipant(
                    janusRoomId,
                    freePort,
                    "Radio Stream"
            );
            log.info("RTP participant {} створено на порту {}", rtpParticipantId, freePort);

            // 3. Надсилаємо команду FFmpeg з payload_type=100
            String rtpUrl = String.format("rtp://janus.rfa:%d?pkt_size=1200", freePort);
            sendFfmpegCommand("start", stationId, sourceStreamUrl, rtpUrl);

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
                // ❌ ВИДАЛЕНО: station.setStationId(String.valueOf(roomId));
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

                    // ❌ ВИДАЛЕНО: station.setStationId(String.valueOf(roomId));
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

                // ✅ ДОДАНО: додаткові параметри для FFmpeg
                message.put("ffmpegOptions", Map.of(
                        "acodec", "libopus",
                        "b:a", "128k",
                        "ar", "48000",
                        "ac", "2",
                        "application", "audio",
                        "vbr", "on",
                        "compression_level", "10"
                ));
            }

            String jsonPayload = objectMapper.writeValueAsString(message);
            log.info("📤 Відправка команди FFmpeg: {}", jsonPayload);

            rabbitTemplate.convertAndSend("ffmpeg-agent-commands", jsonPayload);
            log.info("✅ Команду FFmpeg відправлено в RabbitMQ");

        } catch (Exception e) {
            log.error("❌ Помилка відправки команди FFmpeg: {}", e.getMessage(), e);
            throw new RuntimeException("Помилка відправки команди FFmpeg: " + e.getMessage());
        }
    }
}