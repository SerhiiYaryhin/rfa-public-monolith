package media.toloka.rfa.webrtc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Оновлений RPC-клієнт для Janus.
 * Оскільки прямі адмін-команди до плагінів заблоковані (error 457),
 * ми використовуємо повний цикл: Create Session -> Attach Handle -> Send Message.
 */
@Service
public class JanusRabbitClient {

    private static final Logger log = LoggerFactory.getLogger(JanusRabbitClient.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitTemplate adminRabbitTemplate;
    private final String janusQueue;

    @Value("${janus.admin_secret}")
    private String adminSecret;

    public JanusRabbitClient(@Qualifier("janusRabbitTemplate") RabbitTemplate rabbitTemplate,
                             @Qualifier("janusAdminRabbitTemplate") RabbitTemplate adminRabbitTemplate,
                             @Value("${rabbitmq.queueJanus:janus-commands}") String janusQueue) {
        this.rabbitTemplate = rabbitTemplate;
        this.adminRabbitTemplate = adminRabbitTemplate;
        this.janusQueue = janusQueue;
    }

    public boolean pingJanus() {
        String transactionId = "ping-" + generateShortUUID();
        Map<String, Object> request = Map.of(
                "janus", "ping",
                "admin_secret", adminSecret,
                "transaction", transactionId
        );
        try {
            Map<String, Object> response = sendAndReceiveAdmin(request);
            return "pong".equals(response.get("janus")) || "success".equals(response.get("janus"));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Створення кімнати через повний цикл API
     */
    public Long createDynamicRoom(String roomDescription) {
        return executeInSession((sessionId, handleId) -> {
            String transactionId = "room-create-" + generateShortUUID();
            Map<String, Object> request = Map.of(
                    "janus", "message",
                    "session_id", sessionId,
                    "handle_id", handleId,
                    "transaction", transactionId,
                    "admin_secret", adminSecret,
                    "body", Map.of(
                            "request", "create",
                            "description", roomDescription,
                            "sampling", 48000,
                            "is_private", false
                    )
            );

            Map<String, Object> response = sendAndReceive(request);
            validatePluginResponse(response);

            Map<String, Object> plugindata = (Map<String, Object>) response.get("plugindata");
            Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
            return ((Number) data.get("room")).longValue();
        });
    }

    /**
     * Створення RTP кімнати через повний цикл API
     */
    public Long createDynamicRoomWithRadio(String roomDescription, Integer rtpPort) {
        return executeInSession((sessionId, handleId) -> {
            String transactionId = "room-rtp-create-" + generateShortUUID();
            Map<String, Object> request = Map.of(
                    "janus", "message",
                    "session_id", sessionId,
                    "handle_id", handleId,
                    "transaction", transactionId,
                    "admin_secret", adminSecret,
                    "body", Map.of(
                            "request", "create",
                            "description", roomDescription,
                            "sampling", 48000,
                            "is_private", false,
                            "rtp_stream", true,
                            "rtp_stream_port", rtpPort,
                            "rtp_stream_codec", "opus",
                            "rtp_stream_sample_rate", 48000,
                            "rtp_stream_channels", 2
                    )
            );

            Map<String, Object> response = sendAndReceive(request);
            validatePluginResponse(response);

            Map<String, Object> plugindata = (Map<String, Object>) response.get("plugindata");
            Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
            return ((Number) data.get("room")).longValue();
        });
    }

    /**
     * Реєстрація токена залишається через Admin API (вона працює як Core command)
     */
    public void registerUserToken(String userToken) {
        String transactionId = "token-reg-" + generateShortUUID();
        Map<String, Object> request = Map.of(
                "janus", "add_token",
                "token", userToken,
                "admin_secret", adminSecret,
                "transaction", transactionId,
                "plugins", List.of("janus.plugin.audiobridge")
        );
        sendAndReceiveAdmin(request);
    }

    // --- ДОПОМІЖНІ МЕТОДИ ДЛЯ СЕСІЙ ---

    @FunctionalInterface
    private interface JanusAction<T> {
        T apply(Long sessionId, Long handleId);
    }

    private <T> T executeInSession(JanusAction<T> action) {
        Long sessionId = null;
        Long handleId = null;
        try {
            // 1. Create Session
            sessionId = createSession();
            // 2. Attach Plugin
            handleId = attachPlugin(sessionId, "janus.plugin.audiobridge");
            // 3. Execute Action
            return action.apply(sessionId, handleId);
        } finally {
            // 4. Destroy Session (Clean up)
            if (sessionId != null) {
                destroySession(sessionId);
            }
        }
    }

    private Long createSession() {
        String transactionId = "session-create-" + generateShortUUID();
        Map<String, Object> request = Map.of(
                "janus", "create",
                "transaction", transactionId,
                "admin_secret", adminSecret
        );
        Map<String, Object> response = sendAndReceive(request);
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        return ((Number) data.get("id")).longValue();
    }

    private Long attachPlugin(Long sessionId, String plugin) {
        String transactionId = "attach-" + generateShortUUID();
        Map<String, Object> request = Map.of(
                "janus", "attach",
                "session_id", sessionId,
                "plugin", plugin,
                "transaction", transactionId,
                "admin_secret", adminSecret
        );
        Map<String, Object> response = sendAndReceive(request);
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        return ((Number) data.get("id")).longValue();
    }

    private void destroySession(Long sessionId) {
        try {
            String transactionId = "destroy-" + generateShortUUID();
            Map<String, Object> request = Map.of(
                    "janus", "destroy",
                    "session_id", sessionId,
                    "transaction", transactionId,
                    "admin_secret", adminSecret
            );
            sendAndReceive(request);
        } catch (Exception e) {
            log.warn("Failed to destroy Janus session {}: {}", sessionId, e.getMessage());
        }
    }

    private void validatePluginResponse(Map<String, Object> response) {
        if (!"success".equals(response.get("janus"))) {
            throw new RuntimeException("Помилка Janus API: " + response);
        }
        Map<String, Object> plugindata = (Map<String, Object>) response.get("plugindata");
        if (plugindata == null) return;
        Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
        if (data != null && data.containsKey("error")) {
            throw new RuntimeException("Помилка плагіна Janus: " + data.get("error"));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sendAndReceive(Map<String, Object> request) {
        Object rawResponse = rabbitTemplate.convertSendAndReceive(request);
        if (rawResponse == null) throw new RuntimeException("Таймаут Janus API");
        return (Map<String, Object>) rawResponse;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sendAndReceiveAdmin(Map<String, Object> request) {
        Object rawResponse = adminRabbitTemplate.convertSendAndReceive(request);
        if (rawResponse == null) throw new RuntimeException("Таймаут Janus Admin API");
        return (Map<String, Object>) rawResponse;
    }

    private String generateShortUUID() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
