package media.toloka.rfa.media.webrtc.service.v2;

import media.toloka.rfa.media.webrtc.model.JanusLiveStream;
import media.toloka.rfa.media.webrtc.service.PortAllocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 💓 Сервіс підтримки активності сесій Janus (Keepalive).
 */
@Service
public class SessionKeepaliveService {

    private static final Logger log = LoggerFactory.getLogger(SessionKeepaliveService.class);

    private final JanusSessionManager sessionManager;
    private final PortAllocationService portService;

    public SessionKeepaliveService(
            JanusSessionManager sessionManager,
            PortAllocationService portService) {
        this.sessionManager = sessionManager;
        this.portService = portService;
    }

    /**
     * 💓 Фоновий планувальник: Запускається кожні 20 секунд.
     * Надсилає запити "keepalive" для всіх активних сесій Plain RTP учасників.
     */
    @Scheduled(fixedDelay = 20000)
    public void keepPersistentSessionsAlive() {
        try {
            List<JanusLiveStream> activeStreams = portService.getAllActiveStreams();

            for (JanusLiveStream stream : activeStreams) {
                if (stream.getJanusSessionId() != null) {
                    log.debug("Heartbeat: Надсилаємо keepalive для сесії {}", stream.getJanusSessionId());
                    sessionManager.keepAlive(stream.getJanusSessionId());
                }
            }
        } catch (Exception e) {
            log.error("Помилка під час надсилання keepalive для сесій Janus: {}", e.getMessage());
        }
    }
}