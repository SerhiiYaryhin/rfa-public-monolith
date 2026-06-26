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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Високорівневий сервіс для керування прямими ефірами через WebRTC (v2).
 *
 * Ключові відмінності від v1:
 * - Використовує JanusRabbitClientOptimized
 * - Автоматичний запуск трансляцій при старті сервера (@EventListener)
 * - createAudioBridgeRoom() налаштовує allow_rtp_participants=true
 * - startBroadcast() створює RTP participant і отримує порт від Janus
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
     * Використовуємо @EventListener(ApplicationReadyEvent.class) замість @PostConstruct,
     * щоб гарантувати, що Spring AMQP повністю ініціалізований.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeBroadcasts() {
        log.info("🚀 Ініціалізація трансляцій при старті сервера...");

        try {
            List<StationDto> onlineStations = stationService.getActiveStations();

            if (onlineStations.isEmpty()) {
                log.info("Немає активних станцій для запуску трансляцій");
                return;
            }

            log.info("Знайдено {} активних станцій", onlineStations.size());

            for (StationDto station : onlineStations) {
                String stationId = String.valueOf(station.getId());
                Long janusRoomId = station.getId();

                Optional<JanusLiveStream> existingStream = portService.getStreamByStation(stationId);

                if (existingStream.isPresent()) {
                    log.info("Станція {} має запис в БД на порту {}. Перевіряємо RTP participant...",
                            station.getName(), existingStream.get().getRtpPort());

                    try {
                        boolean hasRtpParticipant = checkRtpParticipantExists(janusRoomId);

                        if (!hasRtpParticipant) {
                            log.info("RTP participant відсутній в кімнаті {}. Створюємо...", janusRoomId);

                            List<Long> activeRooms = janusClient.getActiveRoomsIds();
                            if (!activeRooms.contains(janusRoomId)) {
                                log.info("Кімната {} відсутня в Janus. Створюємо...", janusRoomId);
                                janusClient.createAudioBridgeRoom(janusRoomId, "Ефір: " + station.getName(), null);
                            }

                            Map<String, Object> rtpInfo = janusClient.createRtpParticipant(janusRoomId, "Radio " + station.getName());
                            Integer newRtpPort = (Integer) rtpInfo.get("rtpPort");
                            Long participantId = (Long) rtpInfo.get("participantId");

                            log.info("✅ RTP participant {} створено для кімнати {} на порту {}",
                                    participantId, janusRoomId, newRtpPort);

                            portService.updateStreamPort(stationId, newRtpPort);

                            String rtpIp = (String) rtpInfo.get("rtpIp");
                            String rtpUrl = String.format("rtp://%s:%d?pkt_size=1200", rtpIp, newRtpPort);
                            sendFfmpegCommand("start", stationId, "http://icecast.rfa:8000/ukraine", rtpUrl);

                        } else {
                            log.info("✅ RTP participant вже існує в кімнаті {}", janusRoomId);
                        }
                    } catch (Exception e) {
                        log.error("❌ Помилка перевірки/створення RTP participant для {}: {}",
                                station.getName(), e.getMessage(), e);
                    }
                } else {
                    log.info("Станція {} не має активної трансляції. Запускаємо...", station.getName());

                    try {
                        startBroadcast(stationId);
                        log.info("✅ Трансляцію для станції {} запущено", station.getName());
                    } catch (Exception e) {
                        log.error("❌ Помилка запуску трансляції для станції {}: {}",
                                station.getName(), e.getMessage(), e);
                    }
                }
            }

            log.info("✅ Ініціалізацію трансляцій завершено");

        } catch (Exception e) {
            log.error("❌ Помилка ініціалізації трансляцій", e);
        }
    }

    /**
     * Перевіряє чи є RTP participant в кімнаті.
     */
    private boolean checkRtpParticipantExists(Long roomId) {
        try {
            List<Map<String, Object>> participants = janusClient.getRoomParticipants(roomId);

            return participants.stream()
                    .anyMatch(p -> {
                        String display = (String) p.get("display");
                        return display != null && (
                                display.contains("Radio") ||
                                        display.contains("Stream") ||
                                        display.contains("Ефір")
                        );
                    });
        } catch (Exception e) {
            log.warn("Не вдалося перевірити учасників кімнати {}: {}", roomId, e.getMessage());
            return false;
        }
    }

    // ============================================================
    // УПРАВЛІННЯ ТРАНЛЯЦІЯМИ
    // ============================================================

    @Transactional
    public JanusLiveStream startBroadcast(String stationId) {
        return portService.getStreamByStation(stationId).orElseGet(() -> {
            log.info("Запуск трансляції для станції: {}", stationId);

            String sourceStreamUrl = "http://icecast.rfa:8000/ukraine";
            Integer freePort = portService.findNextFreePort();
            log.info("Виділено порт для трансляції: {}", freePort);

            Long janusRoomId = Long.parseLong(stationId.replaceAll("[^0-9]", ""));

            // 1. Створюємо кімнату з rtp_stream параметрами для прийому зовнішнього потоку
            janusClient.createAudioBridgeRoom(janusRoomId, "Ефір: " + stationId, freePort);
            log.info("Кімнату {} створено з RTP портом {}", janusRoomId, freePort);

            // 2. Надсилаємо команду FFmpeg на виділений порт
            String rtpUrl = String.format("rtp://janus.rfa:%d?pkt_size=1200", freePort);
            sendFfmpegCommand("start", stationId, sourceStreamUrl, rtpUrl);

            return portService.saveActiveStream(stationId, janusRoomId, freePort, 0);
        });
    }

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

    @Transactional
    public List<StationDto> getActiveRoomsInfo() {
        log.debug("Отримання списку активних кімнат");

        List<StationDto> activeStations = stationService.getActiveStations();
        Set<Long> validRoomIds = activeStations.stream()
                .map(StationDto::getId)
                .collect(Collectors.toSet());

        List<Map<String, Object>> janusRooms;
        try {
            janusRooms = janusClient.getJanusRoomsAndParticipants();
            log.debug("Отримано {} кімнат з Janus", janusRooms.size());
        } catch (Exception e) {
            log.error("Помилка отримання даних з Janus, повертаємо дані з БД", e);
            return activeStations;
        }

        Map<Long, Map<String, Object>> janusRoomsMap = new HashMap<>();
        for (Map<String, Object> jRoom : janusRooms) {
            Number roomIdNum = (Number) jRoom.get("room");
            if (roomIdNum != null) {
                janusRoomsMap.put(roomIdNum.longValue(), jRoom);
            }
        }

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
            List<Long> activeJanusRooms = janusClient.getActiveRoomsIds();

            if (activeJanusRooms != null) {
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

    public boolean pingJanus() {
        return janusClient.pingJanus();
    }

    public String getApiSecret() {
        return janusClient.getApiSecret();
    }

    public Map<String, Object> createRtpParticipant(Long roomId, String display) {
        log.info("Створення RTP participant в кімнаті {} з display '{}'", roomId, display);
        return janusClient.createRtpParticipant(roomId, display);
    }

    private void sendFfmpegCommand(String command, String stationId, String streamUrl, String rtpUrl) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("command", command);
            message.put("stationId", stationId);

            if ("start".equals(command)) {
                message.put("streamUrl", streamUrl);
                message.put("rtpUrl", rtpUrl);

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