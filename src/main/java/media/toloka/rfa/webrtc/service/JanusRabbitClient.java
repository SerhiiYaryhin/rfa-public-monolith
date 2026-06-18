package media.toloka.rfa.webrtc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Оновлений RPC-клієнт для Janus.
 * Оскільки Client API (janus-commands) повертає 403, ми використовуємо 
 * Admin API (janus-admin-commands) для всього циклу роботи.
 */
@Service
public class JanusRabbitClient {

    private static final Logger log = LoggerFactory.getLogger(JanusRabbitClient.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitTemplate adminRabbitTemplate;
    private final String janusQueue;

    @Value("${janus.admin_secret}")
    private String adminSecret;

    @Value("${janus.secret:}")
    private String apiSecret;

    public JanusRabbitClient(@Qualifier("janusRabbitTemplate") RabbitTemplate rabbitTemplate,
                             @Qualifier("janusAdminRabbitTemplate") RabbitTemplate adminRabbitTemplate,
                             @Value("${rabbitmq.queueJanus:janus-commands}") String janusQueue) {
        this.rabbitTemplate = rabbitTemplate;
        this.adminRabbitTemplate = adminRabbitTemplate;
        this.janusQueue = janusQueue;
    }

    public boolean pingJanus() {
        String transactionId = "ping-" + generateShortUUID();
        Map<String, Object> request = new HashMap<>();
        request.put("janus", "ping");
        request.put("admin_secret", adminSecret);
        request.put("transaction", transactionId);

        try {
            Map<String, Object> response = sendAndReceiveAdmin(request);
            return "pong".equals(response.get("janus")) || "success".equals(response.get("janus"));
        } catch (Exception e) {
            log.error("Ping failed: {}", e.getMessage());
            return false;
        }
    }

    public Long createDynamicRoom(String roomDescription) {
        return executeInSession((sessionId, handleId) -> {
            String transactionId = "room-create-" + generateShortUUID();
            Map<String, Object> request = createAdminBaseRequest("message", transactionId);
            request.put("session_id", sessionId);
            request.put("handle_id", handleId);
            request.put("body", Map.of(
                    "request", "create",
                    "description", roomDescription,
                    "sampling", 48000,
                    "is_private", false
            ));

            Map<String, Object> response = sendAndReceiveAdmin(request);
            validatePluginResponse(response);
            return ((Number) ((Map)((Map)response.get("plugindata")).get("data")).get("room")).longValue();
        });
    }

    public Long createDynamicRoomWithRadio(String roomDescription, Integer rtpPort) {
        return executeInSession((sessionId, handleId) -> {
            String transactionId = "room-rtp-create-" + generateShortUUID();
            Map<String, Object> request = createAdminBaseRequest("message", transactionId);
            request.put("session_id", sessionId);
            request.put("handle_id", handleId);
            request.put("body", Map.of(
                    "request", "create",
                    "description", roomDescription,
                    "sampling", 48000,
                    "is_private", false,
                    "rtp_stream", true,
                    "rtp_stream_port", rtpPort,
                    "rtp_stream_codec", "opus",
                    "rtp_stream_sample_rate", 48000,
                    "rtp_stream_channels", 2
            ));

            Map<String, Object> response = sendAndReceiveAdmin(request);
            validatePluginResponse(response);
            return ((Number) ((Map)((Map)response.get("plugindata")).get("data")).get("room")).longValue();
        });
    }

    public void registerUserToken(String userToken) {
        String transactionId = "token-reg-" + generateShortUUID();
        Map<String, Object> request = new HashMap<>();
        request.put("janus", "add_token");
        request.put("token", userToken);
        request.put("admin_secret", adminSecret);
        request.put("transaction", transactionId);
        request.put("plugins", List.of("janus.plugin.audiobridge"));
        sendAndReceiveAdmin(request);
    }

    public void toggleMute(Long roomId, Long participantJanusId, boolean mute) {
        executeInSession((sessionId, handleId) -> {
            String transactionId = (mute ? "mute-" : "unmute-") + generateShortUUID();
            Map<String, Object> request = createAdminBaseRequest("message", transactionId);
            request.put("session_id", sessionId);
            request.put("handle_id", handleId);
            request.put("body", Map.of(
                    "request", "configure",
                    "room", roomId,
                    "id", participantJanusId,
                    "muted", mute
            ));
            Map<String, Object> response = sendAndReceiveAdmin(request);
            validatePluginResponse(response);
            return null;
        });
    }

    // --- СЕСІЙНА ЛОГІКА ЧЕРЕЗ ADMIN API ---

    private <T> T executeInSession(JanusAction<T> action) {
        Long sessionId = createSession();
        try {
            Long handleId = attachPlugin(sessionId, "janus.plugin.audiobridge");
            return action.apply(sessionId, handleId);
        } finally {
            destroySession(sessionId);
        }
    }

    private Long createSession() {
        String transactionId = "session-create-" + generateShortUUID();
        Map<String, Object> request = createAdminBaseRequest("create", transactionId);
        Map<String, Object> response = sendAndReceiveAdmin(request);

        if (!"success".equals(response.get("janus"))) {
            throw new RuntimeException("Не вдалося створити сесію Janus через Admin API: " + response);
        }
        return ((Number) ((Map)response.get("data")).get("id")).longValue();
    }

    private Long attachPlugin(Long sessionId, String plugin) {
        String transactionId = "attach-" + generateShortUUID();
        Map<String, Object> request = createAdminBaseRequest("attach", transactionId);
        request.put("session_id", sessionId);
        request.put("plugin", plugin);

        Map<String, Object> response = sendAndReceiveAdmin(request);
        if (!"success".equals(response.get("janus"))) {
            throw new RuntimeException("Не вдалося прикріпити плагін через Admin API: " + response);
        }
        return ((Number) ((Map)response.get("data")).get("id")).longValue();
    }

    private void destroySession(Long sessionId) {
        try {
            String transactionId = "destroy-" + generateShortUUID();
            Map<String, Object> request = createAdminBaseRequest("destroy", transactionId);
            request.put("session_id", sessionId);
            sendAndReceiveAdmin(request);
        } catch (Exception e) {
            log.warn("Session destroy failed: {}", e.getMessage());
        }
    }

    private Map<String, Object> createAdminBaseRequest(String janusType, String transactionId) {
        Map<String, Object> request = new HashMap<>();
        request.put("janus", janusType);
        request.put("transaction", transactionId);
        request.put("admin_secret", adminSecret); // Використовуємо адмін-секрет для всіх команд
        return request;
    }

    private void validatePluginResponse(Map<String, Object> response) {
        if (!"success".equals(response.get("janus"))) {
            throw new RuntimeException("Janus API error: " + response);
        }
        Map plugindata = (Map) response.get("plugindata");
        if (plugindata == null) return;
        Map data = (Map) plugindata.get("data");
        if (data != null && data.containsKey("error")) {
            throw new RuntimeException("Plugin error: " + data.get("error"));
        }
    }

    private Map<String, Object> sendAndReceiveAdmin(Map<String, Object> request) {
        Object res = adminRabbitTemplate.convertSendAndReceive(request);
        if (res == null) throw new RuntimeException("Таймаут Janus Admin API");
        return (Map<String, Object>) res;
    }

    private String generateShortUUID() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @FunctionalInterface
    private interface JanusAction<T> {
        T apply(Long sessionId, Long handleId);
    }
}
