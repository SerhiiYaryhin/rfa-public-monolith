package media.toloka.rfa.media.messanger.webrtc.service.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 🐇 Оптимізований низькорівневий RPC-клієнт для взаємодії з Janus Gateway через RabbitMQ (версія v2).
 * Використовує ручне формування Message для коректного узгодження correlation_id та reply_to
 * з поведінкою Janus Gateway.
 */
@Service
public class JanusRabbitClientOptimized {
    private static final Logger log = LoggerFactory.getLogger(JanusRabbitClientOptimized.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitTemplate adminRabbitTemplate;

    @Value("${janus.admin_secret}")
    private String adminSecret;

    @Value("${janus.secret}")
    private String apiSecret;

    // 🔑 Додаємо черги відповідей, щоб явно вказувати їх у заголовках повідомлень
    @Value("${rabbitmq.queueJanusFrom:from-janus}")
    private String queueJanusFrom;

    @Value("${rabbitmq.queueJanusAdminFrom:from-janus-admin}")
    private String queueJanusAdminFrom;

    public String getApiSecret() {
        return apiSecret;
    }

    public JanusRabbitClientOptimized(
            @Qualifier("janusRabbitTemplate") RabbitTemplate rabbitTemplate,
            @Qualifier("janusAdminRabbitTemplate") RabbitTemplate adminRabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.adminRabbitTemplate = adminRabbitTemplate;
    }

    // ============================================================
    // СТВОРЕННЯ КІМНАТ
    // ============================================================
    public void createAudioBridgeRoom(Long roomId, String name) {
        if (roomId == null) {
            log.warn("Спроба створити кімнату в Janus із порожнім ID (null)");
            return;
        }
        log.info("Створення аудіокімнати в Janus. ID: {}, Назва: {}", roomId, name);
        try {
            executeInSession((sessionId, handleId) -> {
                String transactionId = "room-create-" + generateShortUUID();
                Map<String, Object> request = createBaseRequest("message", transactionId);
                request.put("session_id", sessionId);
                request.put("handle_id", handleId);

                Map<String, Object> body = new HashMap<>();
                body.put("request", "create");
                body.put("room", roomId);
                body.put("description", name);
                body.put("secret", apiSecret);
                body.put("permanent", false);
                body.put("sampling_rate", 48000);
                body.put("audio_codec", "opus");
                body.put("is_private", false);
                body.put("audio_level_average", 25);
                body.put("allow_rtp_participants", true);
                request.put("body", body);

                Map<String, Object> response = sendAndReceive(request);
                validatePluginResponse(response);
                log.info("Аудіокімнату ID: {} ('{}') успішно створено", roomId, name);
                return null;
            });
        } catch (Exception e) {
            log.error("Не вдалося створити кімнату " + roomId + " в Janus: " + e.getMessage(), e);
            throw new RuntimeException("Помилка створення кімнати в Janus", e);
        }
    }

    public void destroyAudioBridgeRoom(Long janusRoomId) {
        if (janusRoomId == null) return;
        log.info("Видалення аудіокімнати ID: {} з Janus", janusRoomId);
        try {
            executeInSession((sessionId, handleId) -> {
                String transactionId = "room-destroy-" + generateShortUUID();
                Map<String, Object> request = createBaseRequest("message", transactionId);
                request.put("session_id", sessionId);
                request.put("handle_id", handleId);

                Map<String, Object> body = new HashMap<>();
                body.put("request", "destroy");
                body.put("room", janusRoomId);
                body.put("secret", apiSecret);
                request.put("body", body);

                Map<String, Object> response = sendAndReceive(request);
                validatePluginResponse(response);
                log.info("Аудіокімнату ID: {} успішно видалено", janusRoomId);
                return null;
            });
        } catch (Exception e) {
            log.error("Не вдалося видалити кімнату " + janusRoomId + " з Janus: " + e.getMessage(), e);
        }
    }

    // ============================================================
    // PLAIN RTP PARTICIPANT
    // ============================================================
    @SuppressWarnings("unchecked")
    public Map<String, Object> joinAsRtpParticipant(Long sessionId, Long handleId, Long roomId, String display, String ip, Integer rtpPort) {
        log.info("Приєднання Plain RTP до кімнати {} на порт джерела {}", roomId, rtpPort);
        String transactionId = "rtp-join-" + generateShortUUID();
        Map<String, Object> request = createBaseRequest("message", transactionId);
        request.put("session_id", sessionId);
        request.put("handle_id", handleId);

        Map<String, Object> body = new HashMap<>();
        body.put("request", "join");
        body.put("room", roomId);
        body.put("display", display);
        body.put("secret", apiSecret);

        Map<String, Object> rtpData = new HashMap<>();
        rtpData.put("ip", ip);
        rtpData.put("port", rtpPort);
        rtpData.put("codec", "opus");
        rtpData.put("pt", 100);
        rtpData.put("rtp_port", 0);
        body.put("rtp", rtpData);
        request.put("body", body);

        Map<String, Object> response = sendAndReceive(request);
        validatePluginResponse(response);
        return response;
    }

    // ============================================================
    // RTP FORWARD
    // ============================================================
    @SuppressWarnings("unchecked")
    public Long createRtpForward(Long roomId, String host, Integer rtpPort) {
        if (roomId == null || rtpPort == null || host == null) {
            throw new IllegalArgumentException("roomId, host та rtpPort не можуть бути null");
        }
        log.info("Створення RTP forward в кімнаті {} на {}:{}", roomId, host, rtpPort);
        return executeInSession((sessionId, handleId) -> {
            String transactionId = "rtp-forward-" + generateShortUUID();
            Map<String, Object> request = createBaseRequest("message", transactionId);
            request.put("session_id", sessionId);
            request.put("handle_id", handleId);

            Map<String, Object> body = new HashMap<>();
            body.put("request", "rtp_forward");
            body.put("room", roomId);
            body.put("secret", apiSecret);
            body.put("host", host);
            body.put("port", rtpPort);
            body.put("ptype", 100);
            body.put("ssrc", roomId * 1000);
            request.put("body", body);

            Map<String, Object> response = sendAndReceive(request);
            validatePluginResponse(response);
            Map<String, Object> plugindata = (Map<String, Object>) response.get("plugindata");
            Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
            return ((Number) data.get("stream_id")).longValue();
        });
    }

    public void stopRtpForward(Long roomId, Long streamId) {
        if (roomId == null || streamId == null) {
            throw new IllegalArgumentException("roomId та streamId не можуть бути null");
        }
        log.info("Зупинка RTP forward в кімнаті {} з stream_id={}", roomId, streamId);
        executeInSession((sessionId, handleId) -> {
            String transactionId = "rtp-stop-" + generateShortUUID();
            Map<String, Object> request = createBaseRequest("message", transactionId);
            request.put("session_id", sessionId);
            request.put("handle_id", handleId);

            Map<String, Object> body = new HashMap<>();
            body.put("request", "stop_rtp_forward");
            body.put("room", roomId);
            body.put("stream_id", streamId);
            body.put("secret", apiSecret);
            request.put("body", body);

            Map<String, Object> response = sendAndReceive(request);
            validatePluginResponse(response);
            log.info("RTP forward успішно зупинено");
            return null;
        });
    }

    // ============================================================
    // ОТРИМАННЯ ДАНИХ КІМНАТ ТА УЧАСНИКІВ
    // ============================================================
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getJanusRoomsAndParticipants() {
        return executeInSession((sessionId, handleId) -> {
            String transactionId = "rooms-list-" + generateShortUUID();
            Map<String, Object> request = createBaseRequest("message", transactionId);
            request.put("session_id", sessionId);
            request.put("handle_id", handleId);
            request.put("body", Map.of("request", "list"));

            Map<String, Object> response = sendAndReceive(request);
            validatePluginResponse(response);

            Map<String, Object> plugindata = (Map<String, Object>) response.get("plugindata");
            if (plugindata == null) return List.of();
            Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
            if (data == null) return List.of();
            List<Map<String, Object>> rooms = (List<Map<String, Object>>) data.get("list");
            if (rooms == null) return List.of();

            for (Map<String, Object> room : rooms) {
                try {
                    Number roomIdNum = (Number) room.get("room");
                    if (roomIdNum == null) continue;
                    Long roomId = roomIdNum.longValue();

                    String partTransactionId = "part-list-" + generateShortUUID();
                    Map<String, Object> partRequest = createBaseRequest("message", partTransactionId);
                    partRequest.put("session_id", sessionId);
                    partRequest.put("handle_id", handleId);
                    partRequest.put("body", Map.of("request", "listparticipants", "room", roomId));

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

    @SuppressWarnings("unchecked")
    public List<Long> getActiveRoomsIds() {
        try {
            return executeInSession((sessionId, handleId) -> {
                String transactionId = "rooms-ids-" + generateShortUUID();
                Map<String, Object> request = createBaseRequest("message", transactionId);
                request.put("session_id", sessionId);
                request.put("handle_id", handleId);
                request.put("body", Map.of("request", "list"));

                Map<String, Object> response = sendAndReceive(request);
                validatePluginResponse(response);
                Map<String, Object> plugindata = (Map<String, Object>) response.get("plugindata");
                Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
                List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("list");
                if (list == null) return List.of();

                return list.stream()
                        .map(m -> ((Number) m.get("room")).longValue())
                        .collect(Collectors.toList());
            });
        } catch (Exception e) {
            log.error("Помилка отримання ID кімнат Janus: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    public boolean isUserInAudioRoom(Long roomId, String userToken) {
        return executeInSession((sessionId, handleId) -> {
            String transactionId = "check-user-" + generateShortUUID();
            Map<String, Object> request = createBaseRequest("message", transactionId);
            request.put("session_id", sessionId);
            request.put("handle_id", handleId);
            request.put("body", Map.of("request", "listparticipants", "room", roomId));

            try {
                Map<String, Object> response = sendAndReceive(request);
                validatePluginResponse(response);
                Map<String, Object> plugindata = (Map<String, Object>) response.get("plugindata");
                if (plugindata == null) return false;
                Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
                if (data == null || data.get("participants") == null) return false;

                List<Map<String, Object>> participants = (List<Map<String, Object>>) data.get("participants");
                for (Map<String, Object> p : participants) {
                    String participantToken = (String) p.get("token");
                    if (userToken.equals(participantToken)) {
                        return true;
                    }
                }
            } catch (Exception e) {
                log.warn("Не вдалося отримати список учасників для кімнати " + roomId, e);
            }
            return false;
        });
    }

    // ============================================================
    // МОДЕРАЦІЯ
    // ============================================================
    @SuppressWarnings("unchecked")
    public void kickUserFromAudioBridgeRoom(Long roomId, String userToken) {
        log.info("Спроба видалити користувача з токеном {} з кімнати {}", userToken, roomId);
        try {
            executeInSession((sessionId, handleId) -> {
                String listTransaction = "list-p-" + generateShortUUID();
                Map<String, Object> listRequest = createBaseRequest("message", listTransaction);
                listRequest.put("session_id", sessionId);
                listRequest.put("handle_id", handleId);
                listRequest.put("body", Map.of("request", "listparticipants", "room", roomId));

                Map<String, Object> listResponse = sendAndReceive(listRequest);
                validatePluginResponse(listResponse);

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
                    String kickTransaction = "kick-u-" + generateShortUUID();
                    Map<String, Object> kickRequest = createBaseRequest("message", kickTransaction);
                    kickRequest.put("session_id", sessionId);
                    kickRequest.put("handle_id", handleId);
                    kickRequest.put("body", Map.of("request", "kick", "room", roomId, "id", janusParticipantId));

                    Map<String, Object> kickResponse = sendAndReceive(kickRequest);
                    validatePluginResponse(kickResponse);
                    log.info("Користувача видалено з кімнати {}", roomId);
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Помилка kick: " + e.getMessage(), e);
        }
    }

    public void muteParticipant(Long roomId, Long participantId, boolean mute) {
        log.info("Модерація: {} учасника {} в кімнаті {}", (mute ? "мутування" : "розмутування"), participantId, roomId);
        executeInSession((sessionId, handleId) -> {
            String transactionId = (mute ? "mute-" : "unmute-") + generateShortUUID();
            Map<String, Object> request = createBaseRequest("message", transactionId);
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
                Map<String, Object> response = sendAndReceive(request);
                validatePluginResponse(response);
                log.info("✅ Успішно змінено мікрофон з секретом");
            } catch (Exception e) {
                log.warn("⚠️ Помилка зміни мікрофона з секретом, пробуємо без нього. Причина: {}", e.getMessage());
                body.remove("secret");
                String retryTransactionId = (mute ? "mute-retry-" : "unmute-retry-") + generateShortUUID();
                request.put("transaction", retryTransactionId);
                Map<String, Object> responseRetry = sendAndReceive(request);
                validatePluginResponse(responseRetry);
                log.info("✅ Успішно змінено мікрофон без секрету");
            }
            return null;
        });
    }

    public void kickParticipant(Long roomId, Long participantId) {
        log.info("Модерація: вигнання учасника {} з кімнати {}", participantId, roomId);
        executeInSession((sessionId, handleId) -> {
            String transactionId = "kick-" + generateShortUUID();
            Map<String, Object> request = createBaseRequest("message", transactionId);
            request.put("session_id", sessionId);
            request.put("handle_id", handleId);

            Map<String, Object> body = new HashMap<>();
            body.put("request", "kick");
            body.put("room", roomId);
            body.put("id", participantId);
            body.put("secret", apiSecret);
            request.put("body", body);

            try {
                Map<String, Object> response = sendAndReceive(request);
                validatePluginResponse(response);
                log.info("✅ Успішно вигнано учасника з секретом");
            } catch (Exception e) {
                log.warn("⚠️ Помилка вигнання з секретом, пробуємо без нього. Причина: {}", e.getMessage());
                body.remove("secret");
                String retryTransactionId = "kick-retry-" + generateShortUUID();
                request.put("transaction", retryTransactionId);
                Map<String, Object> responseRetry = sendAndReceive(request);
                validatePluginResponse(responseRetry);
                log.info("✅ Успішно вигнано учасника без секрету");
            }
            return null;
        });
    }

    // ============================================================
    // ADMIN API
    // ============================================================
    public boolean pingJanus() {
        String transactionId = "ping-" + generateShortUUID();
        Map<String, Object> request = Map.of("janus", "ping", "admin_secret", adminSecret, "transaction", transactionId);
        try {
            Map<String, Object> response = sendAndReceiveAdmin(request);
            return "pong".equals(response.get("janus"));
        } catch (Exception e) {
            log.error("Помилка ping до Janus Gateway: {}", e.getMessage());
            return false;
        }
    }

    public void registerUserToken(String token) {
        log.info("Реєстрація токена користувача в Janus: {}", token);
        String transactionId = "token-add-" + generateShortUUID();
        Map<String, Object> request = Map.of("janus", "add_token", "token", token, "admin_secret", adminSecret, "transaction", transactionId);
        try {
            Map<String, Object> response = sendAndReceiveAdmin(request);
            if (!"success".equals(response.get("janus"))) {
                throw new RuntimeException("Помилка реєстрації токена: " + response);
            }
            log.info("Токен користувача {} успішно зареєстровано в Janus", token);
        } catch (Exception e) {
            log.error("Помилка реєстрації токена користувача: " + e.getMessage(), e);
            throw new RuntimeException("Помилка реєстрації токена в Janus", e);
        }
    }

    @SuppressWarnings("unchecked")
    public boolean checkUserTokenInJanus(String token) {
        String transactionId = "tokens-list-" + generateShortUUID();
        Map<String, Object> request = Map.of("janus", "list_tokens", "admin_secret", adminSecret, "transaction", transactionId);
        try {
            Map<String, Object> response = sendAndReceiveAdmin(request);
            if (!"success".equals(response.get("janus"))) {
                return false;
            }
            Map<String, Object> tokensData = (Map<String, Object>) response.get("data");
            List<Map<String, Object>> tokensList = (List<Map<String, Object>>) tokensData.get("tokens");
            if (tokensList == null) return false;
            for (Map<String, Object> t : tokensList) {
                if (token.equals(t.get("token"))) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("Помилка перевірки списку токенів Janus: " + e.getMessage(), e);
        }
        return false;
    }

    // ============================================================
    // ВНУТРІШНІ ДОПОМІЖНІ МЕТОДИ
    // ============================================================
    private Map<String, Object> createBaseRequest(String janusType, String transactionId) {
        Map<String, Object> request = new HashMap<>();
        request.put("janus", janusType);
        request.put("transaction", transactionId);
        request.put("apisecret", apiSecret);
        return request;
    }

    @SuppressWarnings("unchecked")
    private void validatePluginResponse(Map<String, Object> response) {
        if (!"success".equals(response.get("janus"))) {
            throw new RuntimeException("Janus API error: " + response);
        }
        Map<String, Object> plugindata = (Map<String, Object>) response.get("plugindata");
        if (plugindata == null) return;
        Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
        if (data != null && (data.containsKey("error") || data.containsKey("error_code"))) {
            throw new RuntimeException("Plugin error: " + (data.get("error") != null ? data.get("error") : data.get("error_code")));
        }
    }

    /**
     * 🔑 ВИПРАВЛЕНИЙ МЕТОД: Ручне формування повідомлення для коректного узгодження з Janus.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> sendAndReceive(Map<String, Object> request) {
        String transactionId = (String) request.get("transaction");

        // 1. Створюємо властивості повідомлення
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        // 2. Встановлюємо correlation_id рівним transaction_id (так очікує Janus)
        properties.setCorrelationId(transactionId);
        // 3. Явно вказуємо чергу, в яку Janus має відправити відповідь
        properties.setReplyTo(queueJanusFrom);

        // 4. Конвертуємо Map у Message
        Message message = rabbitTemplate.getMessageConverter().toMessage(request, properties);

        // 5. Відправляємо і чекаємо відповідь (SimpleMessageListenerContainer перехопить її з queueJanusFrom)
        Message reply = rabbitTemplate.sendAndReceive(message);

        if (reply == null) {
            log.error("❌ Таймаут Janus API. Відповідь не отримана в чергу: {}", queueJanusFrom);
            throw new RuntimeException("Таймаут Janus API");
        }

        Object converted = rabbitTemplate.getMessageConverter().fromMessage(reply);
        if (converted instanceof Map) {
            return (Map<String, Object>) converted;
        }
        throw new RuntimeException("Невірний формат відповіді від Janus API");
    }

    /**
     * 🔑 ВИПРАВЛЕНИЙ МЕТОД: Ручне формування повідомлення для Admin API.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> sendAndReceiveAdmin(Map<String, Object> request) {
        String transactionId = (String) request.get("transaction");

        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setCorrelationId(transactionId);
        properties.setReplyTo(queueJanusAdminFrom);

        Message message = adminRabbitTemplate.getMessageConverter().toMessage(request, properties);
        Message reply = adminRabbitTemplate.sendAndReceive(message);

        if (reply == null) {
            log.error("❌ Таймаут Janus Admin API. Відповідь не отримана в чергу: {}", queueJanusAdminFrom);
            throw new RuntimeException("Таймаут Janus Admin API");
        }

        Object converted = adminRabbitTemplate.getMessageConverter().fromMessage(reply);
        if (converted instanceof Map) {
            return (Map<String, Object>) converted;
        }
        throw new RuntimeException("Невірний формат відповіді від Janus Admin API");
    }

    private String generateShortUUID() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @FunctionalInterface
    private interface JanusAction<T> {
        T apply(Long sessionId, Long handleId);
    }

    private <T> T executeInSession(JanusAction<T> action) {
        Long sessionId = createSession();
        try {
            Long handleId = attachPlugin(sessionId, "janus.plugin.audiobridge");
            return action.apply(sessionId, handleId);
        } finally {
            destroySession(sessionId);
        }
    }

    @SuppressWarnings("unchecked")
    public Long attachPlugin(Long sessionId, String plugin) {
        String transactionId = "attach-" + generateShortUUID();
        Map<String, Object> request = createBaseRequest("attach", transactionId);
        request.put("session_id", sessionId);
        request.put("plugin", plugin);
        Map<String, Object> response = sendAndReceive(request);
        if (!"success".equals(response.get("janus"))) {
            throw new RuntimeException("Помилка прикріплення плагіна: " + response);
        }
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        return ((Number) data.get("id")).longValue();
    }

    @SuppressWarnings("unchecked")
    public Long createSession() {
        String transactionId = "session-create-" + generateShortUUID();
        Map<String, Object> request = createBaseRequest("create", transactionId);
        Map<String, Object> response = sendAndReceive(request);
        if (!"success".equals(response.get("janus"))) {
            throw new RuntimeException("Помилка створення сесії: " + response);
        }
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        return ((Number) data.get("id")).longValue();
    }

    public void destroySession(Long sessionId) {
        try {
            String transactionId = "destroy-" + generateShortUUID();
            Map<String, Object> request = createBaseRequest("destroy", transactionId);
            request.put("session_id", sessionId);
            sendAndReceive(request);
        } catch (Exception e) {
            log.warn("Destroy session failed: {}", e.getMessage());
        }
    }

    public void keepAlive(Long sessionId) {
        try {
            String transactionId = "keepalive-" + generateShortUUID();
            Map<String, Object> request = createBaseRequest("keepalive", transactionId);
            request.put("session_id", sessionId);
            sendAndReceive(request);
        } catch (Exception e) {
            log.warn("Keepalive failed for session {}: {}", sessionId, e.getMessage());
        }
    }
}