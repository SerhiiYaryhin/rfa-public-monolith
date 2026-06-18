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
            Map<String, Object> request = createBaseRequest("message", transactionId);
            request.put("session_id", sessionId);
            request.put("handle_id", handleId);
            request.put("body", Map.of(
                    "request", "create",
                    "description", roomDescription,
                    "sampling", 48000,
                    "is_private", false
            ));

            Map<String, Object> response = sendAndReceive(request);
            validatePluginResponse(response);
            return ((Number) ((Map)((Map)response.get("plugindata")).get("data")).get("room")).longValue();
        });
    }

    public Long createDynamicRoomWithRadio(String roomDescription, Integer rtpPort) {
        return executeInSession((sessionId, handleId) -> {
            String transactionId = "room-rtp-create-" + generateShortUUID();
            Map<String, Object> request = createBaseRequest("message", transactionId);
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

            Map<String, Object> response = sendAndReceive(request);
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

    // --- СЕСІЙНА ЛОГІКА ---

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
        
        // Спробуємо спочатку з apiSecret
        Map<String, Object> request = createBaseRequest("create", transactionId);
        Map<String, Object> response = sendAndReceive(request);

        // Якщо 403, спробуємо з adminSecret як apisecret
        if ("error".equals(response.get("janus"))) {
            Map<String, Object> error = (Map<String, Object>) response.get("error");
            if (error != null && ((Number)error.get("code")).intValue() == 403) {
                log.warn("403 з apiSecret, пробуємо з adminSecret...");
                request.put("apisecret", adminSecret);
                request.put("api_secret", adminSecret); // на випадок іншої версії
                response = sendAndReceive(request);
            }
        }

        // Якщо все ще 403, спробуємо БЕЗ секрету
        if ("error".equals(response.get("janus"))) {
            Map<String, Object> error = (Map<String, Object>) response.get("error");
            if (error != null && ((Number)error.get("code")).intValue() == 403) {
                log.warn("403 з adminSecret, пробуємо БЕЗ секрету...");
                request.remove("apisecret");
                request.remove("api_secret");
                response = sendAndReceive(request);
            }
        }

        if (!"success".equals(response.get("janus"))) {
            throw new RuntimeException("Не вдалося створити сесію Janus: " + response);
        }
        
        return ((Number) ((Map)response.get("data")).get("id")).longValue();
    }

    private Long attachPlugin(Long sessionId, String plugin) {
        String transactionId = "attach-" + generateShortUUID();
        Map<String, Object> request = createBaseRequest("attach", transactionId);
        request.put("session_id", sessionId);
        request.put("plugin", plugin);

        Map<String, Object> response = sendAndReceive(request);
        if (!"success".equals(response.get("janus"))) {
            throw new RuntimeException("Не вдалося прикріпити плагін: " + response);
        }
        return ((Number) ((Map)response.get("data")).get("id")).longValue();
    }

    private void destroySession(Long sessionId) {
        try {
            String transactionId = "destroy-" + generateShortUUID();
            Map<String, Object> request = createBaseRequest("destroy", transactionId);
            request.put("session_id", sessionId);
            sendAndReceive(request);
        } catch (Exception e) {
            log.warn("Session destroy failed: {}", e.getMessage());
        }
    }

    private Map<String, Object> createBaseRequest(String janusType, String transactionId) {
        Map<String, Object> request = new HashMap<>();
        request.put("janus", janusType);
        request.put("transaction", transactionId);
        if (apiSecret != null && !apiSecret.isEmpty()) {
            request.put("apisecret", apiSecret);
            request.put("api_secret", apiSecret); 
        }
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

    private Map<String, Object> sendAndReceive(Map<String, Object> request) {
        Object res = rabbitTemplate.convertSendAndReceive(request);
        if (res == null) throw new RuntimeException("Таймаут Janus API");
        return (Map<String, Object>) res;
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
