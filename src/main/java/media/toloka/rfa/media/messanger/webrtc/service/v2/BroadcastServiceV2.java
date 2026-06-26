package media.toloka.rfa.media.messanger.webrtc.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import media.toloka.rfa.media.messanger.webrtc.model.JanusLiveStream;
import media.toloka.rfa.media.messanger.webrtc.model.dto.StationDto;
import media.toloka.rfa.media.messanger.webrtc.service.PortAllocationService;
import media.toloka.rfa.radio.station.service.StationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Сервіс для керування зовнішніми трансляціями (Icecast → FFmpeg → Janus RTP).
 *
 * Відповідальність:
 * - Запуск/зупинка трансляцій радіостанцій
 * - Створення RTP participant'ів для прийому зовнішніх потоків
 * - Автоматичне відновлення трансляцій при старті сервера
 * - Відправка команд FFmpeg агенту через RabbitMQ
 */
@Service
public class BroadcastServiceV2 {

    private static final Logger log = LoggerFactory.getLogger(BroadcastServiceV2.class);

    private static final String DEFAULT_STREAM_URL = "http://icecast.rfa:8000/ukraine";

    private final JanusRabbitClientOptimized janusClient;
    private final PortAllocationService portService;
    private final StationService stationService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public BroadcastServiceV2(
            JanusRabbitClientOptimized janusClient,
            PortAllocationService portService,
            StationService stationService,
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper) {
        this.janusClient = janusClient;
        this.portService = portService;
        this.stationService = stationService;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    // ============================================================
    // АВТОМАТИЧНА ІНІЦІАЛІЗАЦІЯ ПРИ СТАРТІ СЕРВЕРА
    // ============================================================

    /**
     * ✅ ВИПРАВЛЕНО: Використовуємо ApplicationReadyEvent замість @PostConstruct.
     * Це гарантує, що Spring AMQP повністю ініціалізований перед виконанням.
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
                            recreateRtpParticipant(station, janusRoomId);
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

    // ============================================================
    // УПРАВЛІННЯ ТРАНЛЯЦІЯМИ
    // ============================================================

    /**
     * Запуск трансляції радіостанції в WebRTC ефір.
     * Flow: Створення кімнати → RTP participant → FFmpeg → Збереження в БД
     *
     * @param stationId ID станції (наприклад, "352" або "802")
     * @return Дані про створений стрім
     */
    @Transactional
    public JanusLiveStream startBroadcast(String stationId) {
        return portService.getStreamByStation(stationId).orElseGet(() -> {
            log.info("Запуск трансляції для станції: {}", stationId);

            Long janusRoomId = Long.parseLong(stationId.replaceAll("[^0-9]", ""));

            // 1. Створюємо кімнату з allow_rtp_participants=true
            janusClient.createAudioBridgeRoom(janusRoomId, "Ефір: " + stationId, null);
            log.info("Кімнату {} створено з allow_rtp_participants=true", janusRoomId);

            // 2. Створюємо RTP participant і отримуємо порт від Janus
            Map<String, Object> rtpInfo = janusClient.createRtpParticipant(janusRoomId, "Radio Stream");
            Integer rtpPort = (Integer) rtpInfo.get("rtpPort");
            String rtpIp = (String) rtpInfo.get("rtpIp");
            Long participantId = (Long) rtpInfo.get("participantId");

            log.info("RTP participant {} створено. Janus слухає на {}:{}",
                    participantId, rtpIp, rtpPort);

            // 3. Надсилаємо команду FFmpeg на порт, який повернув Janus
            String rtpUrl = String.format("rtp://%s:%d?pkt_size=1200", rtpIp, rtpPort);
            sendFfmpegCommand("start", stationId, DEFAULT_STREAM_URL, rtpUrl);

            // 4. Зберігаємо стан у БД
            return portService.saveActiveStream(stationId, janusRoomId, rtpPort, 0);
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
    // ПЕРЕВІРКА ТА ВІДНОВЛЕННЯ RTP PARTICIPANT
    // ============================================================

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

    /**
     * Перестворює RTP participant для станції після рестарту.
     */
    private void recreateRtpParticipant(StationDto station, Long janusRoomId) {
        try {
            // Перевіряємо чи існує кімната в Janus
            List<Long> activeRooms = janusClient.getActiveRoomsIds();
            if (!activeRooms.contains(janusRoomId)) {
                log.info("Кімната {} відсутня в Janus. Створюємо...", janusRoomId);
                janusClient.createAudioBridgeRoom(janusRoomId, "Ефір: " + station.getName(), null);
            }

            // Створюємо RTP participant
            Map<String, Object> rtpInfo = janusClient.createRtpParticipant(
                    janusRoomId, "Radio " + station.getName());
            Integer newRtpPort = (Integer) rtpInfo.get("rtpPort");
            Long participantId = (Long) rtpInfo.get("participantId");

            log.info("✅ RTP participant {} створено для кімнати {} на порту {}",
                    participantId, janusRoomId, newRtpPort);

            // Оновлюємо запис в БД з новим портом
            String stationId = String.valueOf(janusRoomId);
            portService.updateStreamPort(stationId, newRtpPort);

            // Запускаємо FFmpeg на новий порт
            String rtpIp = (String) rtpInfo.get("rtpIp");
            String rtpUrl = String.format("rtp://%s:%d?pkt_size=1200", rtpIp, newRtpPort);
            sendFfmpegCommand("start", stationId, DEFAULT_STREAM_URL, rtpUrl);

        } catch (Exception e) {
            log.error("❌ Помилка перестворення RTP participant для кімнати {}: {}",
                    janusRoomId, e.getMessage(), e);
            throw e;
        }
    }

    // ============================================================
    // КОМАНДИ FFmpeg
    // ============================================================

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

                // ✅ Параметри для FFmpeg агента
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