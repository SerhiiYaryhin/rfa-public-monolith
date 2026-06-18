package media.toloka.rfa.webrtc.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RPC-клієнт для взаємодії з медіасервером Janus через брокер повідомлень RabbitMQ.
 * Використовує окремі шаблони для звичайного API та Admin API.
 */
@Service
public class JanusRabbitClient {

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
     * Перевірка зв'язку з Janus Admin API (ping).
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
            return false;
        }
    }

    /**
     * Динамічне створення кімнати голосового чату (через Admin API).
     */
    public Long createDynamicRoom(String roomDescription) {
        String transactionId = "room-create-" + generateShortUUID();

        // Для Admin API формат повідомлення до плагіна інший:
        // janus: admin_plugin_message, plugin: ..., request: { ... }
        Map<String, Object> request = Map.of(
                "janus", "admin_plugin_message",
                "admin_secret", adminSecret,
                "plugin", "janus.plugin.audiobridge",
                "transaction", transactionId,
                "request", Map.of(
                        "request", "create",
                        "description", roomDescription,
                        "sampling", 48000,
                        "is_private", false
                )
        );

        Map<String, Object> response = sendAndReceiveAdmin(request);

        // В Admin API відповідь від плагіна приходить у полі "response"
        Map<String, Object> pluginResponse = (Map<String, Object>) response.get("response");
        if (pluginResponse == null || !pluginResponse.containsKey("room")) {
            throw new IllegalStateException("Janus Admin API не повернув ID кімнати. Відповідь: " + response);
        }

        Double roomID = (Double) pluginResponse.get("room");
        return roomID.longValue();
    }

    /**
     * Реєстрація токена доступу (адміністративна команда).
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

        String status = (String) response.get("janus");
        if (!"success".equals(status)) {
            throw new RuntimeException("Не вдалося зареєструвати токен у Janus. Відповідь: " + response);
        }
    }

    /**
     * Керування мікрофоном користувача (через Client API).
     */
    public void toggleMute(Long roomId, Long participantJanusId, boolean mute) {
        String transactionId = (mute ? "mute-" : "unmute-") + generateShortUUID();

        Map<String, Object> request = Map.of(
                "janus", "message",
                "admin_secret", adminSecret,
                "transaction", transactionId,
                "body", Map.of(
                        "request", "configure",
                        "room", roomId,
                        "id", participantJanusId,
                        "muted", mute
                )
        );

        Map<String, Object> response = sendAndReceive(request);

        Map<String, Object> plugindata = (Map<String, Object>) response.get("plugindata");
        if (plugindata != null) {
            Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
            if (data != null && data.containsKey("error")) {
                throw new RuntimeException("Janus AudioBridge повернув помилку: " + data.get("error"));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sendAndReceive(Map<String, Object> request) {
        try {
            // Використовуємо routingKey з налаштувань шаблону
            Object rawResponse = rabbitTemplate.convertSendAndReceive(request);
            if (rawResponse == null) {
                throw new RuntimeException("Таймаут відповіді від Janus API через RabbitMQ (queue: " + janusQueue + ")");
            }
            return (Map<String, Object>) rawResponse;
        } catch (ClassCastException e) {
            throw new RuntimeException("Помилка кастингу відповіді Janus API", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sendAndReceiveAdmin(Map<String, Object> request) {
        try {
            Object rawResponse = adminRabbitTemplate.convertSendAndReceive(request);
            if (rawResponse == null) {
                throw new RuntimeException("Таймаут відповіді від Janus Admin API через RabbitMQ");
            }
            return (Map<String, Object>) rawResponse;
        } catch (ClassCastException e) {
            throw new RuntimeException("Помилка кастингу відповіді Janus Admin API", e);
        }
    }

    private String generateShortUUID() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Створення кімнати з RTP (через Admin API).
     */
    public Long createDynamicRoomWithRadio(String roomDescription, Integer rtpPort) {
        String transactionId = "room-rtp-create-" + generateShortUUID();

        Map<String, Object> request = Map.of(
                "janus", "admin_plugin_message",
                "admin_secret", adminSecret,
                "plugin", "janus.plugin.audiobridge",
                "transaction", transactionId,
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

        Map<String, Object> pluginResponse = (Map<String, Object>) response.get("response");
        if (pluginResponse == null || !pluginResponse.containsKey("room")) {
            throw new IllegalStateException("Janus Admin API не повернув ID RTP-кімнати. Відповідь: " + response);
        }

        Double roomID = (Double) pluginResponse.get("room");
        return roomID.longValue();
    }
}
