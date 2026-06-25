package media.toloka.rfa.media.messanger.webrtc.service.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Оптимізований RPC-клієнт для Janus з batch операціями.
 * Використовує Admin API для отримання всіх даних за мінімальну кількість запитів.
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
    // ОПТИМІЗОВАНІ МЕТОДИ (BATCH OPERATIONS)
    // ============================================================

    /**
     * Отримує ВСІ кімнати з учасниками за ОДИН запит через Admin API.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllRoomsWithParticipants() {
        String transactionId = "all-rooms-" + generateShortUUID();

        Map<String, Object> request = Map.of(
                "janus", "message",
                "plugin", "janus.plugin.audiobridge",
                "transaction", transactionId,
                "admin_secret", adminSecret,
                "body", Map.of("request", "list")
        );

        try {
            Map<String, Object> response = sendAndReceiveAdmin(request);
            Map<String, Object> responseBody = (Map<String, Object>) response.get("response");

            if (responseBody == null) {
                log.warn("Відповідь від Janus не містить блок 'response'");
                return List.of();
            }

            List<Map<String, Object>> rooms = (List<Map<String, Object>>) responseBody.get("list");
            if (rooms == null) {
                return List.of();
            }

            // Для кожної кімнати отримуємо учасників
            for (Map<String, Object> room : rooms) {
                try {
                    Number roomIdNum = (Number) room.get("room");
                    if (roomIdNum == null) continue;

                    List<Map<String, Object>> participants = getRoomParticipants(roomIdNum.longValue());
                    room.put("participants", participants);
                } catch (Exception e) {
                    log.error("Помилка отримання учасників для кімнати " + room.get("room"), e);
                    room.put("participants", List.of());
                }
            }

            return rooms;

        } catch (Exception e) {
            log.error("Помилка отримання списку кімнат з Janus", e);
            return List.of();
        }
    }

    /**
     * Отримує учасників конкретної кімнати через Admin API.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRoomParticipants(Long roomId) {
        String transactionId = "participants-" + generateShortUUID();

        Map<String, Object> request = Map.of(
                "janus", "message",
                "plugin", "janus.plugin.audiobridge",
                "transaction", transactionId,
                "admin_secret", adminSecret,
                "body", Map.of(
                        "request", "listparticipants",
                        "room", roomId
                )
        );

        try {
            Map<String, Object> response = sendAndReceiveAdmin(request);
            Map<String, Object> responseBody = (Map<String, Object>) response.get("response");

            if (responseBody == null || responseBody.get("participants") == null) {
                return List.of();
            }

            return (List<Map<String, Object>>) responseBody.get("participants");

        } catch (Exception e) {
            log.error("Помилка отримання учасників кімнати " + roomId, e);
            return List.of();
        }
    }

    /**
     * Перевіряє, чи є токен користувача в Janus.
     */
    public boolean isTokenRegistered(String userToken) {
        if (userToken == null || userToken.trim().isEmpty()) {
            return false;
        }

        String transactionId = "check-token-" + generateShortUUID();
        Map<String, Object> request = Map.of(
                "janus", "list_tokens",
                "transaction", transactionId,
                "admin_secret", adminSecret
        );

        try {
            Map<String, Object> response = sendAndReceiveAdmin(request);
            Map<String, Object> data = (Map<String, Object>) response.get("data");

            if (data == null) return false;

            List<Map<String, Object>> tokensList = (List<Map<String, Object>>) data.get("tokens");
            if (tokensList == null) return false;

            return tokensList.stream()
                    .map(t -> (String) t.get("token"))
                    .anyMatch(userToken::equals);

        } catch (Exception e) {
            log.error("Помилка перевірки токена в Janus", e);
            return false;
        }
    }

    /**
     * Реєструє токен користувача в Janus.
     */
    public void registerToken(String userToken) {
        String transactionId = "register-token-" + generateShortUUID();
        Map<String, Object> request = Map.of(
                "janus", "add_token",
                "token", userToken,
                "admin_secret", adminSecret,
                "transaction", transactionId,
                "plugins", List.of("janus.plugin.audiobridge")
        );

        try {
            Map<String, Object> response = sendAndReceiveAdmin(request);
            if ("success".equals(response.get("janus"))) {
                log.info("Токен {} успішно зареєстровано в Janus", userToken);
            } else {
                log.warn("Помилка реєстрації токена: {}", response);
            }
        } catch (Exception e) {
            log.error("Помилка реєстрації токена", e);
        }
    }

    /**
     * Створює аудіокімнату через Client API.
     */
    public Long createAudioRoom(Long roomId, String name, Integer rtpPort) {
        return executeInSession((sessionId, handleId) -> {
            String transactionId = "create-room-" + generateShortUUID();
            Map<String, Object> request = createBaseRequest("message", transactionId);
            request.put("session_id", sessionId);
            request.put("handle_id", handleId);
            request.put("body", Map.of(
                    "request", "create",
                    "room", roomId,
                    "description", name,
                    "sampling_rate", 48000,
                    "audio_codec", "opus",
                    "is_private", false,
                    "permanent", false,
                    "audio_level_average", 25
            ));

            Map<String, Object> response = sendAndReceive(request);
            validatePluginResponse(response);

            Map<String, Object> plugindata = (Map<String, Object>) response.get("plugindata");
            Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
            return ((Number) data.get("room")).longValue();
        });
    }

    /**
     * Видаляє аудіокімнату через Client API.
     */
    public void destroyAudioRoom(Long roomId) {
        if (roomId == null) return;

        try {
            executeInSession((sessionId, handleId) -> {
                String transactionId = "destroy-room-" + generateShortUUID();
                Map<String, Object> request = createBaseRequest("message", transactionId);
                request.put("session_id", sessionId);
                request.put("handle_id", handleId);
                request.put("body", Map.of(
                        "request", "destroy",
                        "room", roomId,
                        "permanent", false
                ));

                Map<String, Object> response = sendAndReceive(request);
                validatePluginResponse(response);
                return null;
            });
        } catch (Exception e) {
            log.error("Помилка видалення кімнати " + roomId, e);
        }
    }

    /**
     * Видаляє користувача з кімнати через Admin API.
     */
    public void kickUser(Long roomId, String userToken) {
        if (roomId == null || userToken == null) return;

        try {
            List<Map<String, Object>> participants = getRoomParticipants(roomId);

            Long participantId = participants.stream()
                    .filter(p -> userToken.equals(p.get("token")))
                    .map(p -> ((Number) p.get("id")).longValue())
                    .findFirst()
                    .orElse(null);

            if (participantId == null) {
                log.info("Користувача {} не знайдено в кімнаті {}", userToken, roomId);
                return;
            }

            String transactionId = "kick-" + generateShortUUID();
            Map<String, Object> request = Map.of(
                    "janus", "message",
                    "plugin", "janus.plugin.audiobridge",
                    "transaction", transactionId,
                    "admin_secret", adminSecret,
                    "body", Map.of(
                            "request", "kick",
                            "room", roomId,
                            "id", participantId
                    )
            );

            Map<String, Object> response = sendAndReceiveAdmin(request);
            log.info("Користувача {} видалено з кімнати {}", userToken, roomId);

        } catch (Exception e) {
            log.error("Помилка видалення користувача з кімнати " + roomId, e);
        }
    }

    /**
     * Перевіряє, чи є користувач в кімнаті через Admin API.
     */
    public boolean isUserInRoom(Long roomId, String userToken) {
        List<Map<String, Object>> participants = getRoomParticipants(roomId);
        return participants.stream()
                .anyMatch(p -> userToken.equals(p.get("token")));
    }

    // ============================================================
    // ДОДАТКОВІ МЕТОДИ
    // ============================================================

    /**
     * Отримує список ID усіх активних аудіокімнат у Janus.
     */
    public List<Long> getActiveRoomsIds() {
        String transactionId = "list-rooms-" + generateShortUUID();

        Map<String, Object> request = Map.of(
                "janus", "message",
                "plugin", "janus.plugin.audiobridge",
                "transaction", transactionId,
                "admin_secret", adminSecret,
                "body", Map.of("request", "list")
        );

        List<Long> roomIds = new ArrayList<>();

        try {
            log.info("Запит до Janus на отримання списку активних аудіокімнат...");
            Map<String, Object> response = sendAndReceiveAdmin(request);

            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = (Map<String, Object>) response.get("response");

            if (responseBody == null) {
                log.warn("В відповіді Janus відсутній блок 'response'");
                return roomIds;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> roomsList = (List<Map<String, Object>>) responseBody.get("list");

            if (roomsList == null) {
                log.info("У Janus зараз немає жодної створеної аудіокімнати.");
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
    }

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
            log.error("Помилка ping Janus: {}", e.getMessage());
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

    private Long createSession() {
        String transactionId = "session-" + generateShortUUID();
        Map<String, Object> request = createBaseRequest("create", transactionId);
        Map<String, Object> response = sendAndReceive(request);

        if (!"success".equals(response.get("janus"))) {
            throw new RuntimeException("Помилка створення сесії: " + response);
        }

        return ((Number) ((Map) response.get("data")).get("id")).longValue();
    }

    private void destroySession(Long sessionId) {
        try {
            String transactionId = "destroy-" + generateShortUUID();
            Map<String, Object> request = createBaseRequest("destroy", transactionId);
            request.put("session_id", sessionId);
            sendAndReceive(request);
        } catch (Exception e) {
            log.warn("Помилка знищення сесії: {}", e.getMessage());
        }
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

        return ((Number) ((Map) response.get("data")).get("id")).longValue();
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