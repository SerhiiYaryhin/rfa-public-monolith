package media.toloka.rfa.media.webrtc.service.v2;

import media.toloka.rfa.media.webrtc.model.JanusLiveStream;
import media.toloka.rfa.media.webrtc.model.dto.StationDto;
import media.toloka.rfa.media.webrtc.service.PortAllocationService;
import media.toloka.rfa.radio.station.service.StationService; // ✅ ДОДАНО ЦЕЙ ІМПОРТ
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class BroadcastLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(BroadcastLifecycleService.class);

    @Value("${ffmpeg.agent.host}")
    private String ffmpegAgentHost;

    private final JanusRoomService roomService;
    private final JanusParticipantService participantService;
    private final JanusSessionManager sessionManager;
    private final PortAllocationService portService;
    private final FfmpegCommandService ffmpegService;
    private final StationService stationService;

    public BroadcastLifecycleService(
            JanusRoomService roomService,
            JanusParticipantService participantService,
            JanusSessionManager sessionManager,
            PortAllocationService portService,
            FfmpegCommandService ffmpegService,
            StationService stationService) {
        this.roomService = roomService;
        this.participantService = participantService;
        this.sessionManager = sessionManager;
        this.portService = portService;
        this.ffmpegService = ffmpegService;
        this.stationService = stationService;
    }

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
                        log.error("❌ Помилка запуску трансляції для станції {}: {}", station.getName(), e.getMessage(), e);
                    }
                } else {
                    log.info("Станція {} вже має активну трансляцію на порту {}", station.getName(), existingStream.get().getRtpPort());
                }
            }
            log.info("✅ Ініціалізацію трансляцій завершено");
        } catch (Exception e) {
            log.error("❌ Помилка ініціалізації трансляцій", e);
        }
    }

    @Transactional
    public JanusLiveStream startBroadcast(String stationId) {
        return portService.getStreamByStation(stationId).orElseGet(() -> {
            log.info("🎬 === startBroadcast() === stationId={}", stationId);
            String sourceStreamUrl = "http://icecast.rfa:8000/ukraine";

            Integer freePort = portService.findNextFreePort();
            log.info("🔌 Виділено UDP порт: {}", freePort);

            Long janusRoomId = Long.parseLong(stationId.replaceAll("[^0-9]", ""));
            log.info("🏠 Janus Room ID: {} (з stationId='{}')", janusRoomId, stationId);

            log.info("📤 [КРОК 1] Створення аудіокімнати в Janus...");
            try {
                roomService.createAudioBridgeRoom(janusRoomId, "Ефір: " + stationId);
                log.info("✅ [КРОК 1] Кімнату {} створено", janusRoomId);
            } catch (Exception e) {
                log.error("❌ [КРОК 1] Помилка створення кімнати {}: {}", janusRoomId, e.getMessage(), e);
                throw e;
            }

            log.info("📤 [КРОК 2] Створення персистентної сесії та приєднання Plain RTP...");
            Long sessionId = null;
            Long handleId = null;
            Integer janusRtpPort = null;

            try {
                sessionId = sessionManager.createSession();
                handleId = sessionManager.attachPlugin(sessionId, "janus.plugin.audiobridge");

                Map<String, Object> joinResponse = participantService.joinAsRtpParticipant(
                        sessionId, handleId, janusRoomId, "Радіо Ефір", ffmpegAgentHost, freePort
                );

                Map<String, Object> plugindata = (Map<String, Object>) joinResponse.get("plugindata");
                Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
                Map<String, Object> rtp = (Map<String, Object>) data.get("rtp");
                janusRtpPort = ((Number) rtp.get("port")).intValue();

                log.info("✅ [КРОК 2] RTP приєднано: session={}, handle={}, port={}", sessionId, handleId, janusRtpPort);
            } catch (Exception e) {
                log.error("❌ [КРОК 2] Помилка приєднання Plain RTP: {}", e.getMessage(), e);
                if (sessionId != null) {
                    try { sessionManager.destroySession(sessionId); } catch (Exception ex) {}
                }
                throw new RuntimeException("Помилка приєднання Plain RTP: " + e.getMessage(), e);
            }

            String rtpUrl = String.format("rtp://janus.rfa:%d?localport=%d&pkt_size=1200", janusRtpPort, freePort);
            log.info("🔗 RTP URL для FFmpeg: {}", rtpUrl);

            log.info("📤 [КРОК 3] Відправка команди START до FFmpeg агента...");
            try {
                ffmpegService.sendStartCommand(stationId, sourceStreamUrl, rtpUrl);
                log.info("✅ [КРОК 3] Команду FFmpeg відправлено в RabbitMQ");
            } catch (Exception e) {
                log.error("❌ [КРОК 3] Помилка відправки команди FFmpeg: {}", e.getMessage(), e);
                if (sessionId != null) {
                    try { sessionManager.destroySession(sessionId); } catch (Exception ex) {}
                }
                throw e;
            }

            log.info("💾 [КРОК 4] Збереження стану в БД...");
            try {
                JanusLiveStream savedStream = portService.saveActiveStream(stationId, janusRoomId, freePort, 0, null);
                savedStream.setJanusSessionId(sessionId);
                savedStream.setJanusHandleId(handleId);
                portService.saveStream(savedStream);

                log.info("🎉 === Трансляцію успішно запущено === stationId={}, rtpPort={}, janusRtpPort={}", stationId, freePort, janusRtpPort);
                return savedStream;
            } catch (Exception e) {
                log.error("❌ [КРОК 4] Помилка збереження в БД: {}", e.getMessage(), e);
                if (sessionId != null) {
                    try { sessionManager.destroySession(sessionId); } catch (Exception ex) {}
                }
                throw e;
            }
        });
    }

    @Transactional
    public void stopBroadcast(String stationId) {
        portService.getStreamByStation(stationId).ifPresent(stream -> {
            log.info("⏹️ Зупинка трансляції: {}", stationId);
            ffmpegService.sendStopCommand(stationId);

            if (stream.getJanusSessionId() != null) {
                try {
                    sessionManager.destroySession(stream.getJanusSessionId());
                    log.info("✅ Сесію Janus {} знищено для Plain RTP", stream.getJanusSessionId());
                } catch (Exception e) {
                    log.warn("⚠️ Помилка знищення сесії Janus: {}", e.getMessage());
                }
            }
            portService.deleteStream(stationId);
        });
    }
}