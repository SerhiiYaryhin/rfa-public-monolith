package media.toloka.rfa.media.messanger.webrtc.service.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Оптимізований RPC-клієнт для Janus (v2).
 *
 * Ключові особливості:
 * - Використовує Client API через executeInSession для роботи з плагінами
 * - Admin API тільки для глобальних команд (list_tokens, add_token, ping)
 * - createAudioBridgeRoom() налаштовує allow_rtp_participants=true
 * - createRtpParticipant() створює RTP participant для прийому зовнішніх потоків
 * - Правильний парсинг відповідей (plugindata.data для Client API)
 * - Синхронний RPC через convertSendAndReceive (як у v1, перевірено працює)
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

    /**
     * Створення аудіокімнати з підтримкою RTP participants.
     * Згідно з документацією Janus: allow_rtp_participants = true дозволяє
     * приймати зовнішні RTP потоки через RTP participants.
     */
    /**
     * Створення аудіокімнати з підтримкою зовнішнього RTP потоку.
     * Використовує rtp_stream параметри для прийому вхідного потоку від FFmpeg.
     *
     * @param roomId ID кімнати
     * @param name Назва кімнати
     * @param rtpPort UDP порт для прийому RTP від FFmpeg
     */
    public void createAudioBridgeRoom(Long roomId, String name, Integer rtpPort) {
        if (roomId == null) {
            log.warn("Спроба створити кімнату в Janus із порожнім ID (null)");
            return;
        }

        log.info("Створення аудіокімнати в Janus. ID: {}, Назва: {}, RTP порт: {}", roomId, name, rtpPort);

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

                // Аудіо налаштування
                body.put("sampling_rate", 48000);
                body.put("audio_codec", "opus");
                body.put("is_private", false);
                body.put("audio_level_average", 25);

                // ✅ ВИПРАВЛЕНО: Використовуємо rtp_stream параметри для прийому зовнішнього потоку
                // Це працює в Janus AudioBridge для вхідних RTP потоків від FFmpeg
                if (rtpPort != null && rtpPort > 0) {
                    body.put("rtp_stream", true);
                    body.put("rtp_stream_port", rtpPort);
                    body.put("rtp_stream_codec", "opus");
                    body.put("rtp_stream_sample_rate", 48000);
                    body.put("rtp_stream_channels", 2);
                }

                request.put("body", body);

                Map<String, Object> response = sendAndReceive(request);
                validatePluginResponse(response);

                log.info("Аудіокімнату ID: {} ('{}') успішно створено з RTP портом {}", roomId, name, rtpPort);
                return null;
            });
        } catch (Exception e) {
            log.error("Не вдалося створити кімнату " + roomId + " в Janus: " + e.getMessage(), e);
            throw new RuntimeException("Помилка створення кімнати в Janus", e);
        }
    }

    /**
     * Видалення аудіокімнати з Janus.
     */
    public void destroyAudioBridgeRoom(Long janusRoomId) {
        if (janusRoomId == null) {
            log.warn("Спроба видалити кімнату з Janus із порожнім ID (null)");
            return;
        }

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
                body.put("permanent", false);
                request.put("body", body);

                Map<String, Object> response = sendAndReceive(request);
                validatePluginResponse(response);

                log.info("Аудіокімнату ID: {} успішно видалено з Janus", janusRoomId);
                return null;
            });
        } catch (Exception e) {
            log.error("Не вдалося видалити кімнату " + janusRoomId + " з Janus: " + e.getMessage(), e);
        }
    }

    // ============================================================
    // RTP PARTICIPANT
    // ============================================================

    /**
     * Створення RTP participant для прийому зовнішнього потоку.
     *
     * Згідно з документацією Janus AudioBridge:
     * - join з rtp об'єктом (БЕЗ ip/port = тільки відправка медіа)
     * - Janus автоматично виділяє UDP порт і повертає його у відповіді
     * - FFmpeg має відправляти RTP на цей порт
     *
     * @param roomId ID кімнати
     * @param display Ім'я учасника (наприклад, "Radio Stream")
     * @return Map з інформацією про RTP participant:
     *         - participantId: ID учасника в Janus
     *         - rtpIp: IP адреса для відправки RTP
     *         - rtpPort: UDP порт для відправки RTP
     *         - payloadType: payload type (100 для Opus)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> createRtpParticipant(Long roomId, String display) {
        if (roomId == null) {
            throw new IllegalArgumentException("roomId не може бути null");
        }

        log.info("Створення RTP participant в кімнаті {} з display '{}'", roomId, display);

        return executeInSession((sessionId, handleId) -> {
            String transactionId = "rtp-participant-" + generateShortUUID();
            Map<String, Object> request = createBaseRequest("message", transactionId);
            request.put("session_id", sessionId);
            request.put("handle_id", handleId);

            Map<String, Object> body = new HashMap<>();
            body.put("request", "join");
            body.put("room", roomId);
            body.put("display", display != null ? display : "Radio Stream");
            body.put("muted", false);

            // RTP об'єкт БЕЗ ip та port = тільки відправка медіа
            Map<String, Object> rtp = new HashMap<>();
            rtp.put("payload_type", 100);
            body.put("rtp", rtp);

            request.put("body", body);

            Map<String, Object> response = sendAndReceive(request);
            validatePluginResponse(response);

            // Отримуємо інформацію про RTP participant з відповіді
            Map<String, Object> plugindata = (Map<String, Object>) response.get("plugindata");
            if (plugindata == null) {
                throw new RuntimeException("plugindata відсутній у відповіді");
            }

            Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
            if (data == null) {
                throw new RuntimeException("data відсутній у plugindata");
            }

            // Janus повертає rtp об'єкт з ip, port, payload_type
            Map<String, Object> rtpInfo = (Map<String, Object>) data.get("rtp");
            if (rtpInfo == null) {
                throw new RuntimeException("rtp інформація відсутня у відповіді");
            }

            Long participantId = ((Number) data.get("id")).longValue();
            String rtpIp = (String) rtpInfo.get("ip");
            Integer rtpPort = ((Number) rtpInfo.get("port")).intValue();
            Integer payloadType = ((Number) rtpInfo.get("payload_type")).intValue();

            log.info("RTP participant створено: ID={}, IP={}, Port={}, PT={}",
                    participantId, rtpIp, rtpPort, payloadType);

            Map<String, Object> result = new HashMap<>();
            result.put("participantId", participantId);
            result.put("rtpIp", rtpIp);
            result.put("rtpPort", rtpPort);
            result.put("payloadType", payloadType);

            return result;
        });
    }

    // ============================================================
    // ОТРИМАННЯ СПИСКУ УЧАСНИКІВ
    // ============================================================

    /**
     * Отримує список учасників конкретної кімнати.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRoomParticipants(Long roomId) {
        return executeInSession((sessionId, handleId) -> {
            String transactionId = "participants-" + generateShortUUID();
            Map<String, Object> request = createBaseRequest("message", transactionId);
            request.put("session_id", sessionId);
            request.put("handle_id", handleId);
            request.put("body", Map.of(
                    "request", "listparticipants",
                    "room", roomId
            ));

            try {
                Map<String, Object> response = sendAndReceive(request);
                validatePluginResponse(response);

                Map<String, Object> plugindata = (Map<String, Object>) response.get("plugindata");
                if (plugindata == null) return List.of();

                Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
                if (data == null || data.get("participants") == null) return List.of();

                return (List<Map<String, Object>>) data.get("participants");

            } catch (Exception e) {
                log.error("Помилка отримання учасників кімнати " + roomId, e);
                return List.of();
            }
        });
    }

    // ============================================================
    // ОТРИМАННЯ СПИСКУ КІМНАТ
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
            if (plugindata == null) return List.<Map<String, Object>>of();

            Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
            if (data == null) return List.<Map<String, Object>>of();

            List<Map<String, Object>> rooms = (List<Map<String, Object>>) data.get("list");
            if (rooms == null) return List.<Map<String, Object>>of();

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

    @SuppressWarnings("unchecked")
    public List<Long> getActiveRoomsIds() {
        return executeInSession((sessionId, handleId) -> {
            String transactionId = "list-rooms-" + generateShortUUID();
            Map<String, Object> request = createBaseRequest("message", transactionId);
            request.put("session_id", sessionId);
            request.put("handle_id", handleId);
            request.put("body", Map.of("request", "list"));

            List<Long> roomIds = new ArrayList<>();

            try {
                Map<String, Object> response = sendAndReceive(request);
                validatePluginResponse(response);

                Map<String, Object> plugindata = (Map<String, Object>) response.get("plugindata");
                if (plugindata == null) return roomIds;

                Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
                if (data == null) return roomIds;

                List<Map<String, Object>> roomsList = (List<Map<String, Object>>) data.get("list");
                if (roomsList == null) return roomIds;

                for (Map<String, Object> roomData : roomsList) {
                    Object roomIdObj = roomData.get("room");
                    if (roomIdObj instanceof Number) {
                        roomIds.add(((Number) roomIdObj).longValue());
                    }
                }

            } catch (Exception e) {
                log.error("Помилка отримання списку кімнат: {}", e.getMessage(), e);
            }

            return roomIds;
        });
    }

    // ============================================================
    // РОБОТА З КОРИСТУВАЧАМИ
    // ============================================================

    @SuppressWarnings("unchecked")
    public boolean isUserInAudioRoom(Long roomId, String userToken) {
        return executeInSession((sessionId, handleId) -> {
            String transactionId = "check-user-" + generateShortUUID();
            Map<String, Object> request = createBaseRequest("message", transactionId);
            request.put("session_id", sessionId);
            request.put("handle_id", handleId);
            request.put("body", Map.of(
                    "request", "listparticipants",
                    "room", roomId
            ));

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

    @SuppressWarnings("unchecked")
    public void kickUserFromAudioBridgeRoom(Long roomId, String userToken) {
        if (roomId == null || userToken == null) return;

        log.info("Спроба видалити користувача з токеном {} з кімнати {}", userToken, roomId);

        try {
            executeInSession((sessionId, handleId) -> {
                String listTransaction = "list-p-" + generateShortUUID();
                Map<String, Object> listRequest = createBaseRequest("message", listTransaction);
                listRequest.put("session_id", sessionId);
                listRequest.put("handle_id", handleId);
                listRequest.put("body", Map.of(
                        "request", "listparticipants",
                        "room", roomId
                ));

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
                    kickRequest.put("body", Map.of(
                            "request", "kick",
                            "room", roomId,
                            "id", janusParticipantId
                    ));

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

    // ============================================================
    // ADMIN API
    // ============================================================

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

        Map<String, Object> response = sendAndReceiveAdmin(request);
        if ("success".equals(response.get("janus"))) {
            log.info("Токен {} зареєстровано", userToken);
        }
    }

    @SuppressWarnings("unchecked")
    public Set<String> getAllUsersTokenFromJanus() {
        String transactionId = "user-token-list-" + generateShortUUID();
        Map<String, Object> request = Map.of(
                "janus", "list_tokens",
                "transaction", transactionId,
                "admin_secret", adminSecret
        );

        Map<String, Object> response = sendAndReceiveAdmin(request);
        Set<String> setTokens = new HashSet<>();

        Map<String, Object> tokensData = (Map<String, Object>) response.get("data");
        if (tokensData == null) return setTokens;

        List<Map<String, Object>> tokensList = (List<Map<String, Object>>) tokensData.get("tokens");
        if (tokensList == null) return setTokens;

        for (Map<String, Object> tokenInfo : tokensList) {
            String tokenStr = (String) tokenInfo.get("token");
            if (tokenStr != null) setTokens.add(tokenStr);
        }

        return setTokens;
    }

    public Boolean checkUserTokenInJanus(String uuidUser) {
        if (uuidUser == null || uuidUser.trim().isEmpty()) return false;

        try {
            Set<String> activeTokens = getAllUsersTokenFromJanus();
            return activeTokens.contains(uuidUser);
        } catch (Exception e) {
            log.error("Помилка перевірки токена: " + e.getMessage(), e);
            return false;
        }
    }

    // ============================================================
    // ДОПОМІЖНІ МЕТОДИ
    // ============================================================

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
    private Long createSession() {
        String transactionId = "session-create-" + generateShortUUID();
        Map<String, Object> request = createBaseRequest("create", transactionId);
        Map<String, Object> response = sendAndReceive(request);

        if (!"success".equals(response.get("janus"))) {
            throw new RuntimeException("Помилка створення сесії: " + response);
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
            log.warn("Destroy session failed: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Long attachPlugin(Long sessionId, String plugin) {
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

    private Map<String, Object> createBaseRequest(String janusType, String transactionId) {
        Map<String, Object> request = new HashMap<>();
        request.put("janus", janusType);
        request.put("transaction", transactionId);
        request.put("apisecret", apiSecret);
        return request;
    }

    private void validatePluginResponse(Map<String, Object> response) {
        if (!"success".equals(response.get("janus"))) {
            throw new RuntimeException("Janus API error: " + response);
        }
    }

    /**
     * Синхронний RPC — перевірений робочий механізм з v1.
     */
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