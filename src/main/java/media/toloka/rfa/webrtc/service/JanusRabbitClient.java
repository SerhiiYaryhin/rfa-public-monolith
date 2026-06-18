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
            return false;
        }
    }

    public Long createDynamicRoom(String roomDescription) {
        return executeInSession((sessionId, handleId) -> {
            String transactionId = "room-create-" + generateShortUUID();
            Map<String, Object> body = Map.of(
                    "request", "create",
                    "description", roomDescription,
                    "sampling", 48000,
                    "is_private", false
            );
            
            Map<String, Object> request = createBaseRequest("message", transactionId);
            request.put("session_id", sessionId);
            request.put("handle_id", handleId);
            request.put("body", body);

            Map<String, Object> response = sendAndReceive(request);
            validatePluginResponse(response);

            Map<String, Object> plugindata = (Map<String, Object>) response.get("plugindata");
            Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
            return ((Number) data.get("room")).longValue();
        });
    }

    public Long createDynamicRoomWithRadio(String roomDescription, Integer rtpPort) {
        return executeInSession((sessionId, handleId) -> {
            String transactionId = "room-rtp-create-" + generateShortUUID();
            Map<String, Object> body = Map.of(
                    "request", "create",
                    "description", roomDescription,
                    "sampling", 48000,
                    "is_private", false,
                    "rtp_stream", true,
                    "rtp_stream_port", rtpPort,
                    "rtp_stream_codec", "opus",
                    "rtp_stream_sample_rate", 48000,
                    "rtp_stream_channels", 2
            );

            Map<String, Object> request = createBaseRequest("message", transactionId);
            request.put("session_id", sessionId);
            request.put("handle_id", handleId);
            request.put("body", body);

            Map<String, Object> response = sendAndReceive(request);
            validatePluginResponse(response);

            Map<String, Object> plugindata = (Map<String, Object>) response.get("plugindata");
            Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
            return ((Number) data.get("room")).longValue();
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
        Long sessionId = null;
        Long handleId = null;
        try {
            sessionId = createSession();
            handleId = attachPlugin(sessionId, "janus.plugin.audiobridge");
            return action.apply(sessionId, handleId);
        } finally {
            if (sessionId != null) {
                destroySession(sessionId);
            }
        }
    }

    private Long createSession() {
        String transactionId = "session-create-" + generateShortUUID();
        Map<String, Object> request = createBaseRequest("create", transactionId);
        
        Map<String, Object> response = sendAndReceive(request);
        if (!"success".equals(response.get("janus"))) {
            throw new RuntimeException("Не вдалося створити сесію Janus. Відповідь: " + response);
        }
        
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data == null) {
            throw new RuntimeException("Janus повернув успіх, але об'єкт 'data' відсутній: " + response);
        }
        return ((Number) data.get("id")).longValue();
    }

    private Long attachPlugin(Long sessionId, String plugin) {
        String transactionId = "attach-" + generateShortUUID();
        Map<String, Object> request = createBaseRequest("attach", transactionId);
        request.put("session_id", sessionId);
        request.put("plugin", plugin);

        Map<String, Object> response = sendAndReceive(request);
        if (!"success".equals(response.get("janus"))) {
            throw new RuntimeException("Не вдалося прикріпити плагін " + plugin + ". Відповідь: " + response);
        }
        
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        return ((Number) data.get("id")).longValue();
    }

    private void destroySession(Long sessionId) {
        try {
            String transactionId = "destroy-" + generateShortUUID();
            Map<String, Object> request = createBaseRequest("destroy", transactionId);
            request.put("session_id", sessionId);
            sendAndReceive(request);
        } catch (Exception e) {
            log.warn("Помилка при видаленні сесії Janus {}: {}", sessionId, e.getMessage());
        }
    }

    private Map<String, Object> createBaseRequest(String janusType, String transactionId) {
        Map<String, Object> request = new HashMap<>();
        request.put("janus", janusType);
        request.put("transaction", transactionId);
        if (apiSecret != null && !apiSecret.isEmpty()) {
            request.put("apisecret", apiSecret);
        }
        return request;
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
        log.debug("Sending to Janus: {}", request);
        Object rawResponse = rabbitTemplate.convertSendAndReceive(request);
        if (rawResponse == null) {
            throw new RuntimeException("Таймаут Janus API (queue: " + janusQueue + ")");
        }
        log.debug("Received from Janus: {}", rawResponse);
        return (Map<String, Object>) rawResponse;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sendAndReceiveAdmin(Map<String, Object> request) {
        log.debug("Sending to Janus Admin: {}", request);
        Object rawResponse = adminRabbitTemplate.convertSendAndReceive(request);
        if (rawResponse == null) {
            throw new RuntimeException("Таймаут Janus Admin API");
        }
        log.debug("Received from Janus Admin: {}", rawResponse);
        return (Map<String, Object>) rawResponse;
    }

    private String generateShortUUID() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @FunctionalInterface
    private interface JanusAction<T> {
        T apply(Long sessionId, Long handleId);
    }
}
