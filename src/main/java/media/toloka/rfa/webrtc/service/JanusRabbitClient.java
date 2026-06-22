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
 * Оптимізований RPC-клієнт для Janus.
 * 
 * Тепер використовує перевірений api_secret ('janusrocks') та 
 * чіткий розподіл черг згідно з конфігурацією RabbitMQ медіасервера.
 */
@Service
public class JanusRabbitClient {

    private static final Logger log = LoggerFactory.getLogger(JanusRabbitClient.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitTemplate adminRabbitTemplate;
    private final String janusQueue;

    @Value("${janus.admin_secret}")
    private String adminSecret;

    @Value("${janus.secret}")
    private String apiSecret;

    public String getApiSecret() {
        return apiSecret;
    }

    public JanusRabbitClient(@Qualifier("janusRabbitTemplate") RabbitTemplate rabbitTemplate,
                             @Qualifier("janusAdminRabbitTemplate") RabbitTemplate adminRabbitTemplate,
                             @Value("${rabbitmq.queueJanus:janus-commands}") String janusQueue) {
        this.rabbitTemplate = rabbitTemplate;
        this.adminRabbitTemplate = adminRabbitTemplate;
        this.janusQueue = janusQueue;
    }

    /**
     * Створення кімнати (через клієнтську чергу з apiSecret)
     */
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
            
            Map plugindata = (Map) response.get("plugindata");
            Map data = (Map) plugindata.get("data");
            return ((Number) data.get("room")).longValue();
        });
    }

    /**
     * Створення RTP кімнати для радіо (через клієнтську чергу)
     */
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
                    "rtp_stream", true,
                    "rtp_stream_port", rtpPort,
                    "rtp_stream_codec", "opus",
                    "rtp_stream_sample_rate", 48000,
                    "rtp_stream_channels", 2
            ));

            Map<String, Object> response = sendAndReceive(request);
            validatePluginResponse(response);
            
            Map plugindata = (Map) response.get("plugindata");
            Map data = (Map) plugindata.get("data");
            return ((Number) data.get("room")).longValue();
        });
    }

    /**
     * Глобальні адмін-команди (через адмінську чергу з adminSecret)
     */
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

    /**
     * Отримання списку кімнат та їхніх учасників за один сеанс зв'язку з Janus.
     * Виконується через Client API.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getJanusRoomsAndParticipants() {
        return executeInSession((sessionId, handleId) -> {
            // Запит на список кімнат
            String transactionId = "rooms-list-" + generateShortUUID();
            Map<String, Object> request = createBaseRequest("message", transactionId);
            request.put("session_id", sessionId);
            request.put("handle_id", handleId);
            request.put("body", Map.of("request", "list"));

            Map<String, Object> response = sendAndReceive(request);
            validatePluginResponse(response);

            Map<String, Object> plugindata = (Map<String, Object>) response.get("plugindata");
            Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
            List<Map<String, Object>> rooms = (List<Map<String, Object>>) data.get("list");
            if (rooms == null) {
                return List.of();
            }

            // Для кожної кімнати запитуємо список учасників
            for (Map<String, Object> room : rooms) {
                try {
                    Number roomIdNum = (Number) room.get("room");
                    if (roomIdNum == null) continue;
                    Long roomId = roomIdNum.longValue();

                    String partTransactionId = "part-list-" + generateShortUUID();
                    Map<String, Object> partRequest = createBaseRequest("message", partTransactionId);
                    partRequest.put("session_id", sessionId);
                    partRequest.put("handle_id", handleId);
                    partRequest.put("body", Map.of(
                            "request", "listparticipants",
                            "room", roomId
                    ));

                    Map<String, Object> partResponse = sendAndReceive(partRequest);
                    validatePluginResponse(partResponse);

                    Map<String, Object> partPluginData = (Map<String, Object>) partResponse.get("plugindata");
                    Map<String, Object> partData = (Map<String, Object>) partPluginData.get("data");
                    List<Map<String, Object>> participants = (List<Map<String, Object>>) partData.get("participants");
                    room.put("participants", participants != null ? participants : List.of());
                } catch (Exception e) {
                    log.error("Помилка отримання учасників для кімнати " + room.get("room"), e);
                    room.put("participants", List.of());
                }
            }
            return rooms;
        });
    }

    public void toggleMute(Long roomId, Long participantJanusId, boolean mute) {
        executeInSession((sessionId, handleId) -> {
            String transactionId = (mute ? "mute-" : "unmute-") + generateShortUUID();
            Map<String, Object> request = createBaseRequest("message", transactionId);
            request.put("session_id", sessionId);
            request.put("handle_id", handleId);
            request.put("body", Map.of(
                    "request", "configure",
                    "room", roomId,
                    "id", participantJanusId,
                    "muted", mute
            ));
            Map<String, Object> response = sendAndReceive(request);
            validatePluginResponse(response);
            return null;
        });
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
        Map<String, Object> request = createBaseRequest("create", transactionId);
        Map<String, Object> response = sendAndReceive(request);

        if (!"success".equals(response.get("janus"))) {
            throw new RuntimeException("Помилка створення сесії: " + response);
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
            throw new RuntimeException("Помилка прикріплення плагіна: " + response);
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
            log.warn("Destroy session failed: {}", e.getMessage());
        }
    }

    private Map<String, Object> createBaseRequest(String janusType, String transactionId) {
        Map<String, Object> request = new HashMap<>();
        request.put("janus", janusType);
        request.put("transaction", transactionId);
        request.put("apisecret", apiSecret); // Використовуємо підтверджений 'janusrocks'
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
