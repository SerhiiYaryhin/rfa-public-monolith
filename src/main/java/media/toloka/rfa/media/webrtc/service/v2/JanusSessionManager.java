package media.toloka.rfa.media.webrtc.service.v2;

import media.toloka.rfa.media.webrtc.service.v2.JanusAmqpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 🎭 Менеджер сесій Janus Gateway.
 * Відповідає за життєвий цикл сесій: створення, прикріплення плагінів, знищення.
 * Забезпечує автоматичне очищення ресурсів через executeInSession().
 */
@Service
public class JanusSessionManager {

    private static final Logger log = LoggerFactory.getLogger(JanusSessionManager.class);

    private final JanusAmqpClient amqpClient;

    @Value("${janus.secret}")
    private String apiSecret;

    public JanusSessionManager(JanusAmqpClient amqpClient) {
        this.amqpClient = amqpClient;
    }

    /**
     * ⚙️ Виконання дій у контексті тимчасової сесії та хендлу плагіна.
     * Автоматично створює сесію та хендл плагіна, виконує задану функцію,
     * а у блоці finally гарантує видалення сесії.
     */
    public <T> T executeInSession(JanusAction<T> action) {
        Long sessionId = createSession();
        try {
            Long handleId = attachPlugin(sessionId, "janus.plugin.audiobridge");
            return action.apply(sessionId, handleId);
        } finally {
            destroySession(sessionId);
        }
    }

    /**
     * Створення нової сесії в Janus Gateway.
     */
    @SuppressWarnings("unchecked")
    public Long createSession() {
        String transactionId = amqpClient.generateTransactionId("session-create");
        Map<String, Object> request = amqpClient.createBaseRequest("create", transactionId, apiSecret);
        Map<String, Object> response = amqpClient.sendAndReceive(request);

        if (!"success".equals(response.get("janus"))) {
            throw new RuntimeException("Помилка створення сесії: " + response);
        }

        Map<String, Object> data = (Map<String, Object>) response.get("data");
        return ((Number) data.get("id")).longValue();
    }

    /**
     * Створення хендлу (handle) підключення плагіна Janus.
     */
    @SuppressWarnings("unchecked")
    public Long attachPlugin(Long sessionId, String plugin) {
        String transactionId = amqpClient.generateTransactionId("attach");
        Map<String, Object> request = amqpClient.createBaseRequest("attach", transactionId, apiSecret);
        request.put("session_id", sessionId);
        request.put("plugin", plugin);

        Map<String, Object> response = amqpClient.sendAndReceive(request);

        if (!"success".equals(response.get("janus"))) {
            throw new RuntimeException("Помилка прикріплення плагіна: " + response);
        }

        Map<String, Object> data = (Map<String, Object>) response.get("data");
        return ((Number) data.get("id")).longValue();
    }

    /**
     * Знищення сесії в Janus Gateway.
     */
    public void destroySession(Long sessionId) {
        try {
            String transactionId = amqpClient.generateTransactionId("destroy");
            Map<String, Object> request = amqpClient.createBaseRequest("destroy", transactionId, apiSecret);
            request.put("session_id", sessionId);
            amqpClient.sendAndReceive(request);
        } catch (Exception e) {
            log.warn("Destroy session failed: {}", e.getMessage());
        }
    }

    /**
     * Надіслати keepalive для підтримки сесії відкритою.
     */
    public void keepAlive(Long sessionId) {
        try {
            String transactionId = amqpClient.generateTransactionId("keepalive");
            Map<String, Object> request = amqpClient.createBaseRequest("keepalive", transactionId, apiSecret);
            request.put("session_id", sessionId);
            amqpClient.sendAndReceive(request);
        } catch (Exception e) {
            log.warn("Keepalive failed for session {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Функціональний інтерфейс для дій у сесії.
     */
    @FunctionalInterface
    public interface JanusAction<T> {
        T apply(Long sessionId, Long handleId);
    }
}