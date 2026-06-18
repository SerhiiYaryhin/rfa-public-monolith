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
 * RPC-клієнт для взаємодії з медіасервером Janus через RabbitMQ.
 * Використовує Admin API для керування кімнатами та токенами.
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

    /**
     * Перевірка зв'язку (Core command)
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
            return "success".equals(response.get("janus")) || "pong".equals(response.get("janus"));
        } catch (Exception e) {
            log.error("Janus ping failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Створення кімнати (Admin Plugin Message)
     */
    public Long createDynamicRoom(String roomDescription) {
        String transactionId = "room-create-" + generateShortUUID();
        
        // Формат для адмін-команд плагінам: janus=admin_plugin_message, plugin=..., request={...}
        Map<String, Object> request = Map.of(
                "janus", "admin_plugin_message",
                "admin_secret", adminSecret,
                "transaction", transactionId,
                "plugin", "janus.plugin.audiobridge",
                "request", Map.of(
                        "request", "create",
                        "description", roomDescription,
                        "sampling", 48000,
                        "is_private", false
                )
        );

        Map<String, Object> response = sendAndReceiveAdmin(request);
        return extractRoomIdFromAdminResponse(response);
    }

    /**
     * Створення RTP кімнати для радіо (Admin Plugin Message)
     */
    public Long createDynamicRoomWithRadio(String roomDescription, Integer rtpPort) {
        String transactionId = "room-rtp-create-" + generateShortUUID();
        
        Map<String, Object> request = Map.of(
                "janus", "admin_plugin_message",
                "admin_secret", adminSecret,
                "transaction", transactionId,
                "plugin", "janus.plugin.audiobridge",
                "request", Map.of(
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

        Map<String, Object> response = sendAndReceiveAdmin(request);
        return extractRoomIdFromAdminResponse(response);
    }

    /**
     * Реєстрація токена (Core command)
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

        Map<String, Object> response = sendAndReceiveAdmin(request);
        if (!"success".equals(response.get("janus"))) {
            throw new RuntimeException("Помилка реєстрації токена: " + response);
        }
    }

    /**
     * Керування мікрофоном (Admin Plugin Message)
     * Дозволяє модератору керувати учасником без створення клієнтської сесії
     */
    public void toggleMute(Long roomId, Long participantJanusId, boolean mute) {
        String transactionId = (mute ? "mute-" : "unmute-") + generateShortUUID();
        Map<String, Object> request = Map.of(
                "janus", "admin_plugin_message",
                "admin_secret", adminSecret,
                "transaction", transactionId,
                "plugin", "janus.plugin.audiobridge",
                "request", Map.of(
                        "request", "configure",
                        "room", roomId,
                        "id", participantJanusId,
                        "muted", mute
                )
        );

        Map<String, Object> response = sendAndReceiveAdmin(request);
        Map<String, Object> pluginResponse = (Map<String, Object>) response.get("response");
        if (pluginResponse != null && pluginResponse.containsKey("error")) {
            throw new RuntimeException("Janus AudioBridge error: " + pluginResponse.get("error"));
        }
    }

    @SuppressWarnings("unchecked")
    private Long extractRoomIdFromAdminResponse(Map<String, Object> response) {
        // У відповіді на admin_plugin_message результат від плагіна лежить у полі "response"
        Map<String, Object> pluginResponse = (Map<String, Object>) response.get("response");
        if (pluginResponse == null || !pluginResponse.containsKey("room")) {
            throw new IllegalStateException("Janus не повернув ID кімнати. Повна відповідь: " + response);
        }
        return ((Number) pluginResponse.get("room")).longValue();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sendAndReceiveAdmin(Map<String, Object> request) {
        try {
            Object rawResponse = adminRabbitTemplate.convertSendAndReceive(request);
            if (rawResponse == null) {
                throw new RuntimeException("Таймаут відповіді від Janus Admin API");
            }
            return (Map<String, Object>) rawResponse;
        } catch (Exception e) {
            log.error("Error communicating with Janus Admin API: {}", e.getMessage());
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sendAndReceive(Map<String, Object> request) {
        Object rawResponse = rabbitTemplate.convertSendAndReceive(request);
        if (rawResponse == null) throw new RuntimeException("Таймаут Janus API");
        return (Map<String, Object>) rawResponse;
    }

    private String generateShortUUID() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
