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
 * Ключові відмінності від v1:
 * - Використовує Client API через executeInSession для роботи з плагінами
 * - Admin API тільки для глобальних команд (list_tokens, add_token, ping)
 * - createAudioBridgeRoom() передає RTP параметри для прийому зовнішнього потоку
 * - Правильний парсинг відповідей (plugindata.data для Client API)
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
    // СТВОРЕННЯ КІМНАТ (Client API через executeInSession)
    // ============================================================

    /**
     * Створення аудіокімнати з явним ID та RTP портом.
     * КРИТИЧНО: передає RTP параметри в Janus для прийому зовнішнього потоку.
     *
     * @param roomId ID кімнати (має збігатися з ID станції з БД)
     * @param name Назва кімнати
     * @param rtpPort UDP порт для прийому RTP потоку (може бути null для звичайної кімнати)
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

                // ✅ КРИТИЧНО: RTP STREAM ПАРАМЕТРИ для прийому зовнішнього потоку
                if (rtpPort != null && rtpPort > 0) {
                    body.put("rtp_stream", true);
                    body.put("rtp_stream_port", rtpPort);
                    body.put("rtp_stream_codec", "opus");
                    body.put("rtp_stream_sample_rate", 48000);
                    body.put("rtp_stream_channels", 2);
                    log.info("Кімната {} налаштована для прийому RTP на порту {}", roomId, rtpPort);
                }

                request.put("body", body);

                Map<String, Object> response = sendAndReceive(request);
                validatePluginResponse(response);

                log.info("Аудіокімнату ID: {} ('{}') успішно створено з RTP на порту {}", roomId, name, rtpPort);
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
    // ОТРИМАННЯ СПИСКУ КІМНАТ (Client API через executeInSession)
    // ============================================================

    /**
     * Отримання списку кімнат та їхніх учасників за один сеанс зв'язку з Janus.
     */
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
            if (plugindata == null) {
                log.warn("plugindata відсутній у відповіді");
                return List.<Map<String, Object>>of();
            }

            Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
            if (data == null) {
                log.warn("data відсутній у plugindata");
                return List.<Map<String, Object>>of();
            }

            List<Map<String, Object>> rooms = (List<Map<String, Object>>) data.get("list");
            if (rooms == null) {
                return List.<Map<String, Object>>of();
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

    /**
     * Отримує список ID усіх активних аудіокімнат у Janus.
     */
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
                log.info("Запит до Janus на отримання списку активних аудіокімнат...");
                Map<String, Object> response = sendAndReceive(request);
                validatePluginResponse(response);

                Map<String, Object> plugindata = (Map<String, Object>) response.get("plugindata");
                if (plugindata == null) return roomIds;

                Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
                if (data == null) return roomIds;

                List<Map<String, Object>> roomsList = (List<Map<String, Object>>) data.get("list");
                if (roomsList == null) {
                    log.info("У Janus немає жодної створеної аудіокімнати.");
                    return roomIds;
                }

                for (Map<String, Object> roomData : roomsList) {
                    Object roomIdObj = roomData.get("room");
                    if (roomIdObj instanceof Number) {
                        roomIds.add(((Number) roomIdObj).longValue());
                    }
                }

                log.info("Успішно отримано список активних кімнат з Janus. Знайдено кімнат: {}", roomIds.size());

            } catch (Exception e) {
                log.error("Помилка під час отримання списку активних кімнат з Janus: {}", e.getMessage(), e);
            }

            return roomIds;
        });
    }

    // ============================================================
    // РОБОТА З КОРИСТУВАЧАМИ (Client API через executeInSession)
    // ============================================================

    /**
     * Перевіряє, чи є користувач в кімнаті.
     */
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

                @SuppressWarnings("unchecked")
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

    /**
     * Примусово видаляє користувача з кімнати (kick).
     */
    @SuppressWarnings("unchecked")
    public void kickUserFromAudioBridgeRoom(Long roomId, String userToken) {
        if (roomId == null || userToken == null) {
            log.warn("Неможливо виконати kick: roomId або userToken дорівнює null");
            return;
        }

        log.info("Спроба видалити користувача з токеном {} з кімнати {}", userToken, roomId);

        try {
            executeInSession((sessionId, handleId) -> {
                // КРОК 1: Отримуємо список учасників
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

                if (data == null || data.get("participants") == null) {
                    log.info("У кімнаті {} немає активних учасників", roomId);
                    return null;
                }

                List<Map<String, Object>> participants = (List<Map<String, Object>>) data.get("participants");
                Long janusParticipantId = null;

                for (Map<String, Object> participant : participants) {
                    String pToken = (String) participant.get("token");
                    if (userToken.equals(pToken)) {
                        janusParticipantId = ((Number) participant.get("id")).longValue();
                        break;
                    }
                }

                // КРОК 2: Виконуємо kick
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

                    log.info("Надсилаємо команду kick для учасника ID: {} у кімнаті {}", janusParticipantId, roomId);
                    Map<String, Object> kickResponse = sendAndReceive(kickRequest);
                    validatePluginResponse(kickResponse);

                    log.info("Користувача з токеном {} (Janus ID: {}) успішно видалено з кімнати {}",
                            userToken, janusParticipantId, roomId);
                } else {
                    log.info("Користувача з токеном {} не знайдено в кімнаті {}", userToken, roomId);
                }

                return null;
            });
        } catch (Exception e) {
            log.error("Помилка при виконанні kick для кімнати " + roomId + ": " + e.getMessage(), e);
        }
    }

    // ============================================================
    // ADMIN API (тільки для глобальних команд)
    // ============================================================

    /**
     * Перевірка зв'язку з Janus Admin API.
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

    /**
     * Реєстрація токена користувача в Janus.
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
        if ("success".equals(response.get("janus"))) {
            log.info("Токен {} успішно зареєстровано в Janus", userToken);
        } else {
            log.warn("Помилка реєстрації токена: {}", response);
        }
    }

    /**
     * Отримання списку всіх зареєстрованих токенів.
     */
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
        if (tokensData == null) {
            log.warn("Відповідь від Janus не містить блоку 'data'");
            return setTokens;
        }

        List<Map<String, Object>> tokensList = (List<Map<String, Object>>) tokensData.get("tokens");
        if (tokensList == null) {
            log.warn("Блок 'data' не містить масиву 'tokens'");
            return setTokens;
        }

        for (Map<String, Object> tokenInfo : tokensList) {
            String tokenStr = (String) tokenInfo.get("token");
            if (tokenStr != null) {
                setTokens.add(tokenStr);
            }
        }

        log.info("Успішно отримано {} токенів з Janus", setTokens.size());
        return setTokens;
    }

    /**
     * Перевірка наявності токена користувача в Janus.
     */
    public Boolean checkUserTokenInJanus(String uuidUser) {
        if (uuidUser == null || uuidUser.trim().isEmpty()) {
            log.warn("Спроба перевірити порожній UUID користувача");
            return false;
        }

        log.info("Перевірка токена користувача (UUID: {}) в Janus", uuidUser);
        try {
            Set<String> activeTokens = getAllUsersTokenFromJanus();
            boolean isRegistered = activeTokens.contains(uuidUser);

            if (isRegistered) {
                log.info("Токен (UUID: {}) знайдено в Janus", uuidUser);
            } else {
                log.warn("Токена (UUID: {}) НЕМАЄ в Janus", uuidUser);
            }

            return isRegistered;
        } catch (Exception e) {
            log.error("Помилка під час перевірки токена " + uuidUser + " в Janus: " + e.getMessage(), e);
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