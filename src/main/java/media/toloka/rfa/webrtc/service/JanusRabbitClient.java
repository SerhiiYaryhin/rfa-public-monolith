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
 * Клієнт для взаємодії з медіасервером Janus через RabbitMQ.
 * 
 * Цей клас реалізує складну логіку керування сесіями Janus через брокер повідомлень.
 * Оскільки прямі адміністративні запити до плагінів часто обмежені налаштуваннями транспорту (error 457),
 * клієнт використовує стандартний життєвий цикл Janus API:
 * 1. Створення сесії (Session Create)
 * 2. Прикріплення до плагіна (Plugin Attach)
 * 3. Надсилання команд (Plugin Message)
 * 4. Завершення сесії (Session Destroy)
 */
@Service
public class JanusRabbitClient {

    private static final Logger log = LoggerFactory.getLogger(JanusRabbitClient.class);

    // Шаблон для стандартних команд (клієнтське API)
    private final RabbitTemplate rabbitTemplate;
    // Шаблон для адміністративних команд (адмін API)
    private final RabbitTemplate adminRabbitTemplate;
    
    // Назва черги для вхідних команд Janus
    private final String janusQueue;

    // Майстер-пароль адміністратора Janus (з файлу janus.jcfg)
    @Value("${janus.admin_secret}")
    private String adminSecret;

    // Секретний ключ API (якщо налаштований у janus.jcfg)
    @Value("${janus.secret:}")
    private String apiSecret;

    public JanusRabbitClient(@Qualifier("janusRabbitTemplate") RabbitTemplate rabbitTemplate,
                             @Qualifier("janusAdminRabbitTemplate") RabbitTemplate adminRabbitTemplate,
                             @Value("${rabbitmq.queueJanus:janus-commands}") String janusQueue) {
        this.rabbitTemplate = rabbitTemplate;
        this.adminRabbitTemplate = adminRabbitTemplate;
        this.janusQueue = janusQueue;
    }

    /**
     * Перевірка зв'язку з Janus через адміністративний канал.
     * @return true, якщо Janus відповів "pong" або "success"
     */
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
            log.error("Помилка Ping Janus: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Створення динамічної голосової кімнати (audiobridge).
     * Використовує повний сесійний цикл для гарантованого проходження команди.
     */
    public Long createDynamicRoom(String roomDescription) {
        return executeInSession((sessionId, handleId) -> {
            String transactionId = "room-create-" + generateShortUUID();
            Map<String, Object> request = createAuthRequest("message", transactionId);
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
            
            // Витягуємо ID створеної кімнати з глибин JSON-відповіді
            Map plugindata = (Map) response.get("plugindata");
            Map data = (Map) plugindata.get("data");
            return ((Number) data.get("room")).longValue();
        });
    }

    /**
     * Створення кімнати з підтримкою зовнішнього RTP-потоку.
     * Це дозволяє "заливати" в кімнату звук з FFmpeg (наприклад, ефір радіостанції).
     */
    public Long createDynamicRoomWithRadio(String roomDescription, Integer rtpPort) {
        return executeInSession((sessionId, handleId) -> {
            String transactionId = "room-rtp-create-" + generateShortUUID();
            Map<String, Object> request = createAuthRequest("message", transactionId);
            request.put("session_id", sessionId);
            request.put("handle_id", handleId);
            request.put("body", Map.of(
                    "request", "create",
                    "description", roomDescription,
                    "sampling", 48000,
                    "is_private", false,
                    "rtp_stream", true,           // Вмикаємо прийом RTP
                    "rtp_stream_port", rtpPort,   // Порт, який Janus відкриє для нас
                    "rtp_stream_codec", "opus",   // Кодек Opus (стандарт WebRTC)
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
     * Реєстрація токена безпеки в Janus Core.
     * Без реєстрації токена клієнти не зможуть підключитися, якщо в Janus увімкнено token auth.
     */
    public void registerUserToken(String userToken) {
        String transactionId = "token-reg-" + generateShortUUID();
        Map<String, Object> request = new HashMap<>();
        request.put("janus", "add_token");
        request.put("token", userToken);
        request.put("admin_secret", adminSecret);
        request.put("transaction", transactionId);
        request.put("plugins", List.of("janus.plugin.audiobridge"));
        
        // Реєстрація токенів — це завжди глобальна адмін-команда
        sendAndReceiveAdmin(request);
    }

    /**
     * Керування станом мікрофона учасника (Mute/Unmute).
     * Використовується для модерації ефіру.
     */
    public void toggleMute(Long roomId, Long participantJanusId, boolean mute) {
        executeInSession((sessionId, handleId) -> {
            String transactionId = (mute ? "mute-" : "unmute-") + generateShortUUID();
            Map<String, Object> request = createAuthRequest("message", transactionId);
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

    // --- ВНУТРІШНЯ СЕСІЙНА ЛОГІКА ---

    /**
     * Виконує дію в межах тимчасової сесії Janus.
     * Гарантує створення сесії, прикріплення плагіна та коректне видалення сесії після завершення.
     */
    private <T> T executeInSession(JanusAction<T> action) {
        Long sessionId = createSession();
        try {
            Long handleId = attachPlugin(sessionId, "janus.plugin.audiobridge");
            return action.apply(sessionId, handleId);
        } finally {
            // Завжди видаляємо сесію, щоб не вичерпати ліміти Janus
            destroySession(sessionId);
        }
    }

    /**
     * Створення нової сесії в Janus.
     */
    private Long createSession() {
        String transactionId = "session-create-" + generateShortUUID();
        Map<String, Object> request = createAuthRequest("create", transactionId);
        Map<String, Object> response = sendAndReceive(request);

        if (!"success".equals(response.get("janus"))) {
            throw new RuntimeException("Помилка створення сесії: " + response);
        }
        return ((Number) ((Map)response.get("data")).get("id")).longValue();
    }

    /**
     * Прикріплення до конкретного плагіна в межах сесії.
     */
    private Long attachPlugin(Long sessionId, String plugin) {
        String transactionId = "attach-" + generateShortUUID();
        Map<String, Object> request = createAuthRequest("attach", transactionId);
        request.put("session_id", sessionId);
        request.put("plugin", plugin);

        Map<String, Object> response = sendAndReceive(request);
        if (!"success".equals(response.get("janus"))) {
            throw new RuntimeException("Помилка прикріплення плагіна: " + response);
        }
        return ((Number) ((Map)response.get("data")).get("id")).longValue();
    }

    /**
     * Видалення сесії.
     */
    private void destroySession(Long sessionId) {
        try {
            String transactionId = "destroy-" + generateShortUUID();
            Map<String, Object> request = createAuthRequest("destroy", transactionId);
            request.put("session_id", sessionId);
            sendAndReceive(request);
        } catch (Exception e) {
            log.warn("Не вдалося видалити сесію {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Створює базовий об'єкт запиту з набором ключів авторизації.
     * Для надійності передає одночасно apisecret, api_secret та admin_secret.
     */
    private Map<String, Object> createAuthRequest(String janusType, String transactionId) {
        Map<String, Object> request = new HashMap<>();
        request.put("janus", janusType);
        request.put("transaction", transactionId);
        
        if (apiSecret != null && !apiSecret.isEmpty()) {
            request.put("apisecret", apiSecret);
            request.put("api_secret", apiSecret);
        }
        // Використовуємо адмін-секрет як універсальний ключ, якщо він налаштований
        request.put("admin_secret", adminSecret); 
        
        return request;
    }

    /**
     * Перевіряє, чи не повернув плагін внутрішню помилку.
     */
    private void validatePluginResponse(Map<String, Object> response) {
        if (!"success".equals(response.get("janus"))) {
            throw new RuntimeException("Помилка Janus API: " + response);
        }
        Map plugindata = (Map) response.get("plugindata");
        if (plugindata == null) return;
        Map data = (Map) plugindata.get("data");
        if (data != null && data.containsKey("error")) {
            throw new RuntimeException("Внутрішня помилка плагіна: " + data.get("error"));
        }
    }

    /**
     * Надсилає повідомлення в користувацьку чергу RabbitMQ і чекає на відповідь.
     */
    private Map<String, Object> sendAndReceive(Map<String, Object> request) {
        Object res = rabbitTemplate.convertSendAndReceive(request);
        if (res == null) throw new RuntimeException("Таймаут Janus API (janus-commands)");
        return (Map<String, Object>) res;
    }

    /**
     * Надсилає повідомлення в адміністративну чергу RabbitMQ і чекає на відповідь.
     */
    private Map<String, Object> sendAndReceiveAdmin(Map<String, Object> request) {
        Object res = adminRabbitTemplate.convertSendAndReceive(request);
        if (res == null) throw new RuntimeException("Таймаут Janus Admin API (janus-admin-commands)");
        return (Map<String, Object>) res;
    }

    /**
     * Генерація короткого унікального ID транзакції.
     */
    private String generateShortUUID() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @FunctionalInterface
    private interface JanusAction<T> {
        T apply(Long sessionId, Long handleId);
    }
}
