package media.toloka.rfa.media.webrtc.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import media.toloka.rfa.media.messanger.service.MessangerService;
import media.toloka.rfa.media.webrtc.model.JanusLiveStream;
import media.toloka.rfa.media.webrtc.model.dto.StationDto;
import media.toloka.rfa.media.webrtc.service.PortAllocationService;
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
 * ✅ ВИПРАВЛЕНО: Використовує механізм rtp_forward замість allow_rtp_participants.
 * Це працює стабільно через convertSendAndReceive() без проблем з ACK.
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

//    @Transactional
//    public JanusLiveStream startBroadcast(String stationId) {
//        return portService.getStreamByStation(stationId).orElseGet(() -> {
//            log.info("🎬 === startBroadcast() === stationId={}", stationId);
//
//            String sourceStreamUrl = "http://icecast.rfa:8000/ukraine";
//            log.info("📻 URL потоку станції: {}", sourceStreamUrl);
//
//            Long janusRoomId = Long.parseLong(stationId.replaceAll("[^0-9]", ""));
//            log.info("🏠 Janus Room ID: {}", janusRoomId);
//
//            // Крок 1: Створюємо кімнату з allow_rtp_participants=true
//            log.info("📤 [КРОК 1] Створення аудіокімнати в Janus...");
//            try {
//                janusClient.createAudioBridgeRoom(janusRoomId, "Ефір: " + stationId);
//                log.info("✅ [КРОК 1] Кімнату {} створено", janusRoomId);
//            } catch (Exception e) {
//                log.error("❌ [КРОК 1] Помилка створення кімнати {}: {}", janusRoomId, e.getMessage(), e);
//                throw e;
//            }
//
//            // Крок 2: ✅ НОВЕ - Створюємо RTP учасника
//            log.info("📤 [КРОК 2] Створення RTP учасника для станції {}...", stationId);
//            Map<String, Object> rtpParticipant;
//            try {
//                rtpParticipant = janusClient.createRtpParticipant(
//                        janusRoomId,
//                        stationId,
//                        "Радіостанція " + stationId
//                );
//                log.info("✅ [КРОК 2] RTP учасника створено: ip={}, port={}",
//                        rtpParticipant.get("ip"), rtpParticipant.get("port"));
//            } catch (Exception e) {
//                log.error("❌ [КРОК 2] Помилка створення RTP учасника: {}", e.getMessage(), e);
//                throw e;
//            }
//
//            // Крок 3: ✅ Формуємо RTP URL для FFmpeg (отриманий від Janus)
//            String rtpIp = (String) rtpParticipant.get("ip");
//            Integer rtpPort = (Integer) rtpParticipant.get("port");
//            String rtpUrl = String.format("rtp://%s:%d?pkt_size=1200", rtpIp, rtpPort);
//            log.info("🔗 RTP URL для FFmpeg: {}", rtpUrl);
//
//            // Крок 4: Відправка команди FFmpeg
//            log.info("📤 [КРОК 3] Відправка команди START до FFmpeg агента...");
//            try {
//                sendFfmpegCommand("start", stationId, sourceStreamUrl, rtpUrl);
//                log.info("✅ [КРОК 3] Команду FFmpeg відправлено в RabbitMQ");
//            } catch (Exception e) {
//                log.error("❌ [КРОК 3] Помилка відправки команди FFmpeg: {}", e.getMessage(), e);
//                throw e;
//            }
//
//            // Крок 5: Збереження в БД
//            log.info("💾 [КРОК 4] Збереження стану в БД...");
//            try {
//                // ✅ Використовуємо порт, який повернув Janus
//                JanusLiveStream savedStream = portService.saveActiveStream(
//                        stationId, janusRoomId, rtpPort, 0, 0L);
//                log.info("✅ [КРОК 4] Збережено: id={}, stationId={}, janusRoomId={}, rtpPort={}",
//                        savedStream.getId(), savedStream.getStationId(),
//                        savedStream.getJanusRoomId(), savedStream.getRtpPort());
//
//                log.info("🎉 === Трансляцію успішно запущено === stationId={}, rtpPort={}",
//                        stationId, rtpPort);
//
//                return savedStream;
//            } catch (Exception e) {
//                log.error("❌ [КРОК 4] Помилка збереження в БД: {}", e.getMessage(), e);
//                throw e;
//            }
//        });
//    }

    /**
     * ✅ ОНОВЛЕНО: Запуск трансляції через механізм rtp_forward.
     *
     * Flow:
     * 1. Виділяємо UDP порт через PortAllocationService
     * 2. Створюємо звичайну аудіокімнату в Janus
     * 3. Створюємо RTP forward на виділений порт (Janus починає слухати)
     * 4. Відправляємо команду FFmpeg на цей порт
     * 5. Зберігаємо stream_id для подальшої зупинки
     */
    @Transactional
    public JanusLiveStream startBroadcast(String stationId) {
        return portService.getStreamByStation(stationId).orElseGet(() -> {
            log.info("🎬 === startBroadcast() === stationId={}", stationId);

            String sourceStreamUrl = "http://icecast.rfa:8000/ukraine";
            Integer freePort = portService.findNextFreePort();
            log.info("🔌 Виділено UDP порт: {}", freePort);

            Long janusRoomId = Long.parseLong(stationId.replaceAll("[^0-9]", ""));
            log.info("🏠 Janus Room ID: {} (з stationId='{}')", janusRoomId, stationId);

            // Крок 1: Створюємо звичайну кімнату (без rtp_stream, без allow_rtp_participants)
            log.info("📤 [КРОК 1] Створення аудіокімнати в Janus...");
            try {
                janusClient.createAudioBridgeRoom(janusRoomId, "Ефір: " + stationId);
                log.info("✅ [КРОК 1] Кімнату {} створено", janusRoomId);
            } catch (Exception e) {
                log.error("❌ [КРОК 1] Помилка створення кімнати {}: {}", janusRoomId, e.getMessage(), e);
                throw e;
            }

            // Крок 2: Створюємо RTP forward на виділений порт
            log.info("📤 [КРОК 2] Створення RTP forward на порт {}...", freePort);
            Long streamId;
            try {
                streamId = janusClient.createRtpForward(janusRoomId, freePort);
                log.info("✅ [КРОК 2] RTP forward створено: stream_id={}, port={}", streamId, freePort);
            } catch (Exception e) {
                log.error("❌ [КРОК 2] Помилка створення RTP forward: {}", e.getMessage(), e);
                throw e;
            }

            // Крок 3: Формуємо RTP URL для FFmpeg
            String rtpUrl = String.format("rtp://janus.rfa:%d?pkt_size=1200", freePort);
            log.info("🔗 RTP URL для FFmpeg: {}", rtpUrl);

            // Крок 4: Відправка команди FFmpeg агенту
            log.info("📤 [КРОК 3] Відправка команди START до FFmpeg агента...");
            try {
                sendFfmpegCommand("start", stationId, sourceStreamUrl, rtpUrl);
                log.info("✅ [КРОК 3] Команду FFmpeg відправлено в RabbitMQ");
            } catch (Exception e) {
                log.error("❌ [КРОК 3] Помилка відправки команди FFmpeg: {}", e.getMessage(), e);
                throw e;
            }

            // Крок 5: Збереження в БД
            log.info("💾 [КРОК 4] Збереження стану в БД...");
            try {
                JanusLiveStream savedStream = portService.saveActiveStream(
                        stationId, janusRoomId, freePort, 0, streamId);
                log.info("✅ [КРОК 4] Збережено: id={}, stationId={}, janusRoomId={}, rtpPort={}, streamId={}",
                        savedStream.getId(), savedStream.getStationId(),
                        savedStream.getJanusRoomId(), savedStream.getRtpPort(),
                        savedStream.getRtpForwardStreamId());

                log.info("🎉 === Трансляцію успішно запущено === stationId={}, rtpPort={}, streamId={}",
                        stationId, freePort, streamId);

                return savedStream;
            } catch (Exception e) {
                log.error("❌ [КРОК 4] Помилка збереження в БД: {}", e.getMessage(), e);
                throw e;
            }
        });
    }


//    @Transactional
//    public void stopBroadcast(String stationId) {
//        portService.getStreamByStation(stationId).ifPresent(stream -> {
//            log.info("⏹️ Зупинка трансляції: {}", stationId);
//
//            // 1. Зупиняємо FFmpeg
//            sendFfmpegCommand("stop", stationId, null, null);
//
//            // 2. ✅ НОВЕ - Видаляємо RTP учасника
//            try {
//                janusClient.removeRtpParticipant(stream.getJanusRoomId(), stationId);
//                log.info("✅ RTP учасника видалено з кімнати {}", stream.getJanusRoomId());
//            } catch (Exception e) {
//                log.warn("⚠️ Помилка видалення RTP учасника: {}", e.getMessage());
//            }
//
//            // 3. Видаляємо з БД
//            portService.deleteStream(stationId);
//        });
//    }

    /**
     * ✅ ОНОВЛЕНО: Зупинка трансляції через stop_rtp_forward.
     */
    @Transactional
    public void stopBroadcast(String stationId) {
        portService.getStreamByStation(stationId).ifPresent(stream -> {
            log.info("⏹️ Зупинка трансляції: {}", stationId);

            // 1. Зупиняємо FFmpeg
            sendFfmpegCommand("stop", stationId, null, null);

            // 2. Зупиняємо RTP forward (використовуємо rtpForwardStreamId)
            if (stream.getRtpForwardStreamId() != null && stream.getRtpForwardStreamId() > 0) {
                try {
                    janusClient.stopRtpForward(stream.getJanusRoomId(), stream.getRtpForwardStreamId());
                    log.info("✅ RTP forward зупинено: stream_id={}", stream.getRtpForwardStreamId());
                } catch (Exception e) {
                    log.warn("⚠️ Помилка зупинки RTP forward: {}", e.getMessage());
                }
            }

            // 3. Видаляємо з БД
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

        // Синхронізація: видаляємо зайві кімнати з Janus
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

        // Збагачуємо дані станцій інформацією з Janus
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
                    // Створюємо звичайну кімнату
                    janusClient.createAudioBridgeRoom(roomId, station.getName());

                    // Створюємо RTP forward якщо є запис в БД
                    Optional<JanusLiveStream> existingStream = portService.getStreamByStation(stationIdStr);
                    if (existingStream.isPresent()) {
                        Integer rtpPort = existingStream.get().getRtpPort();
                        Long streamId = janusClient.createRtpForward(roomId, rtpPort);
                        portService.updateRtpForwardStreamId(stationIdStr, streamId);
                        log.info("RTP forward відновлено для кімнати {}: stream_id={}", roomId, streamId);
                    }

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