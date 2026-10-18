package media.toloka.rfa.media.messanger.webrtc.service.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 👥 Сервіс управління учасниками аудіокімнат та модерації.
 * Відповідає за приєднання RTP учасників, mute/unmute, kick.
 */
@Service
public class JanusParticipantService {

    private static final Logger log = LoggerFactory.getLogger(JanusParticipantService.class);

    private final JanusAmqpClient amqpClient;
    private final JanusSessionManager sessionManager;

    @Value("${janus.secret}")
    private String apiSecret;

    public JanusParticipantService(JanusAmqpClient amqpClient, JanusSessionManager sessionManager) {
        this.amqpClient = amqpClient;
        this.sessionManager = sessionManager;
    }

    /**
     * 🔌 Приєднання Plain RTP джерела (FFmpeg) як учасника голосової кімнати.
     *
     * @param sessionId Активна сесія Janus
     * @param handleId Хендл прикріпленого плагіна AudioBridge
     * @param roomId ID кімнати
     * @param display Ім'я учасника в списку (напр. "Радіо Ефір")
     * @param ip IP-адреса, на яку Janus чекає RTP (зазвичай 127.0.0.1)
     * @param rtpPort Порт, з якого FFmpeg відправлятиме RTP пакети
     * @return Відповідь Janus API, яка містить порт, який Janus виділив для прийому RTP
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> joinAsRtpParticipant(
            Long sessionId, Long handleId, Long roomId, String display, String ip, Integer rtpPort) {

        log.info("Приєднання Plain RTP до кімнати {} на порт джерела {}", roomId, rtpPort);

        String transactionId = amqpClient.generateTransactionId("rtp-join");
        Map<String, Object> request = amqpClient.createBaseRequest("message", transactionId, apiSecret);
        request.put("session_id", sessionId);
        request.put("handle_id", handleId);

        Map<String, Object> body = new HashMap<>();
        body.put("request", "join");
        body.put("room", roomId);
        body.put("display", display);
        body.put("secret", apiSecret);

        // RTP конфігурація Plain RTP учасника
        Map<String, Object> rtpData = new HashMap<>();
        rtpData.put("ip", ip);
        rtpData.put("port", rtpPort);
        rtpData.put("codec", "opus");
        rtpData.put("pt", 100);       // Стандартний Payload Type для Opus в Janus
        rtpData.put("rtp_port", 0);   // 0 означає автовиділення порту на стороні Janus
        body.put("rtp", rtpData);

        request.put("body", body);

        Map<String, Object> response = amqpClient.sendAndReceive(request);
        amqpClient.validatePluginResponse(response);

        return response;
    }

    /**
     * 🔇 Виселення (kick) користувача за його токеном (UUID).
     */
    @SuppressWarnings("unchecked")
    public void kickUserFromAudioBridgeRoom(Long roomId, String userToken) {
        log.info("Спроба видалити користувача з токеном {} з кімнати {}", userToken, roomId);

        try {
            sessionManager.executeInSession((sessionId, handleId) -> {
                String listTransaction = amqpClient.generateTransactionId("list-p");
                Map<String, Object> listRequest = amqpClient.createBaseRequest("message", listTransaction, apiSecret);
                listRequest.put("session_id", sessionId);
                listRequest.put("handle_id", handleId);
                listRequest.put("body", Map.of("request", "listparticipants", "room", roomId));

                Map<String, Object> listResponse = amqpClient.sendAndReceive(listRequest);
                amqpClient.validatePluginResponse(listResponse);

                Map<String, Object> plugindata = (Map<String, Object>) listResponse.get("plugindata");
                Map<String, Object> data = (Map<String, Object>) plugindata.get("data");

                if (data == null || data.get("participants") == null) return null;

                List<Map<String, Object>> participants = (List<Map<String, Object>>) data.get("participants");
                Long janusParticipantId = null;

                for (Map<String, Object> participant : participants) {
                    String pToken = (String) participant.get("token");
                    if (userToken.equals(pToken)) {
                        janusParticipantId = ((Number) participant.get("id")).longValue();
                        break;
                    }
                }

                if (janusParticipantId != null) {
                    String kickTransaction = amqpClient.generateTransactionId("kick-u");
                    Map<String, Object> kickRequest = amqpClient.createBaseRequest("message", kickTransaction, apiSecret);
                    kickRequest.put("session_id", sessionId);
                    kickRequest.put("handle_id", handleId);
                    kickRequest.put("body", Map.of("request", "kick", "room", roomId, "id", janusParticipantId));

                    Map<String, Object> kickResponse = amqpClient.sendAndReceive(kickRequest);
                    amqpClient.validatePluginResponse(kickResponse);

                    log.info("Користувача видалено з кімнати {}", roomId);
                }

                return null;
            });
        } catch (Exception e) {
            log.error("Помилка kick: {}", e.getMessage(), e);
        }
    }

    /**
     * 🔇 Керування мікрофоном учасника (mute/unmute) модератором.
     * 🌀 Auto-Retry: Спочатку з секретом, при невдачі - без секрету.
     */
    public void muteParticipant(Long roomId, Long participantId, boolean mute) {
        log.info("Модерація: {} учасника {} в кімнаті {}",
                (mute ? "мутування" : "розмутування"), participantId, roomId);

        sessionManager.executeInSession((sessionId, handleId) -> {
            String transactionId = amqpClient.generateTransactionId(mute ? "mute" : "unmute");
            Map<String, Object> request = amqpClient.createBaseRequest("message", transactionId, apiSecret);
            request.put("session_id", sessionId);
            request.put("handle_id", handleId);

            Map<String, Object> body = new HashMap<>();
            body.put("request", "configure");
            body.put("room", roomId);
            body.put("id", participantId);
            body.put("muted", mute);
            body.put("secret", apiSecret);
            request.put("body", body);

            try {
                Map<String, Object> response = amqpClient.sendAndReceive(request);
                amqpClient.validatePluginResponse(response);
                log.info("✅ Успішно змінено мікрофон з секретом");
            } catch (Exception e) {
                log.warn("️ Помилка зміни мікрофона з секретом, пробуємо без нього. Причина: {}", e.getMessage());
                body.remove("secret");
                String retryTransactionId = amqpClient.generateTransactionId(mute ? "mute-retry" : "unmute-retry");
                request.put("transaction", retryTransactionId);
                Map<String, Object> responseRetry = amqpClient.sendAndReceive(request);
                amqpClient.validatePluginResponse(responseRetry);
                log.info("✅ Успішно змінено мікрофон без секрету");
            }

            return null;
        });
    }

    /**
     * ❌ Вигнання учасника з кімнати за його внутрішнім ID в Janus.
     * 🌀 Auto-Retry: Аналогічно мутуванню.
     */
    public void kickParticipant(Long roomId, Long participantId) {
        log.info("Модерація: вигнання учасника {} з кімнати {}", participantId, roomId);

        sessionManager.executeInSession((sessionId, handleId) -> {
            String transactionId = amqpClient.generateTransactionId("kick");
            Map<String, Object> request = amqpClient.createBaseRequest("message", transactionId, apiSecret);
            request.put("session_id", sessionId);
            request.put("handle_id", handleId);

            Map<String, Object> body = new HashMap<>();
            body.put("request", "kick");
            body.put("room", roomId);
            body.put("id", participantId);
            body.put("secret", apiSecret);
            request.put("body", body);

            try {
                Map<String, Object> response = amqpClient.sendAndReceive(request);
                amqpClient.validatePluginResponse(response);
                log.info("✅ Успішно вигнано учасника з секретом");
            } catch (Exception e) {
                log.warn("⚠️ Помилка вигнання з секретом, пробуємо без нього. Причина: {}", e.getMessage());
                body.remove("secret");
                String retryTransactionId = amqpClient.generateTransactionId("kick-retry");
                request.put("transaction", retryTransactionId);
                Map<String, Object> responseRetry = amqpClient.sendAndReceive(request);
                amqpClient.validatePluginResponse(responseRetry);
                log.info("✅ Успішно вигнано учасника без секрету");
            }

            return null;
        });
    }
}