package media.toloka.rfa.media.messanger.webrtc.service.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 🐇 Оптимізований низькорівневий RPC-клієнт для взаємодії з Janus Gateway через RabbitMQ (версія v2).
 *
 * Особливості архітектури:
 * - Використовує клієнтське API плагінів через запуск короткострокових/персистентних сесій (`executeInSession`).
 * - Керування та сигналізація виконуються синхронно за допомогою RabbitMQ черг запитів-відповідей (`convertSendAndReceive`).
 * - Налаштовує аудіокімнати для підтримки Plain RTP потоків (`allow_rtp_participants`).
 * - Дозволяє транслювати кімнати назовні за допомогою `rtp_forward`.
 * - Реалізує автоматичний повтор операцій модерації без передачі пароля для статичних кімнат.
 */
@Service
public class JanusRabbitClientOptimized {

    private static final Logger log = LoggerFactory.getLogger(JanusRabbitClientOptimized.class);

    // RabbitTemplate для стандартних клієнтських операцій (черга 'janus-commands')
    private final RabbitTemplate rabbitTemplate;
    
    // RabbitTemplate для адміністративних операцій (черга 'janus-admin-commands')
    private final RabbitTemplate adminRabbitTemplate;

    // Секретний ключ адміністратора Janus
    @Value("${janus.admin_secret}")
    private String adminSecret;

    // Загальний секретний ключ Janus для сесій користувачів
    @Value("${janus.secret}")
    private String apiSecret;

    /**
     * Повертає загальний секретний ключ Janus.
     */
    public String getApiSecret() {
        return apiSecret;
    }

    /**
     * Конструктор з впровадженням спеціалізованих шаблонів черг AMQP.
     */
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
     * 🎙️ Створення нової голосової кімнати в Janus AudioBridge.
     * Параметр allow_rtp_participants встановлюється в true для прийому Plain RTP потоку від FFmpeg.
     */
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
                body.put("secret", apiSecret); // Встановлюємо пароль кімнати рівним apiSecret
                body.put("permanent", false);

                // Налаштування аудіо
                body.put("sampling_rate", 48000);
                body.put("audio_codec", "opus");
                body.put("is_private", false);
                body.put("audio_level_average", 25);

                // Дозволяємо RTP-підключення (для нашого FFmpeg агента)
                body.put("allow_rtp_participants", true);

                request.put("body", body);

                Map<String, Object> response = sendAndReceive(request);
                validatePluginResponse(response);

                log.info("Аудіокімнату ID: {} ('{}') успішно створено з allow_rtp_participants=true", roomId, name);
                return null;
            });
        } catch (Exception e) {
            log.error("Не вдалося створити кімнату " + roomId + " в Janus: " + e.getMessage(), e);
            throw new RuntimeException("Помилка створення кімнати в Janus", e);
        }
    }

    /**
     * 💥 Видалення аудіокімнати з Janus.
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
                body.put("secret", apiSecret); // Передаємо пароль кімнати для підтвердження прав

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
    // PLAIN RTP PARTICIPANT
    // ============================================================

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

        String transactionId = "rtp-join-" + generateShortUUID();
        Map<String, Object> request = createBaseRequest("message", transactionId);
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

        Map<String, Object> response = sendAndReceive(request);
        validatePluginResponse(response);

        return response;
    }

    // ============================================================
    // RTP FORWARD (ТРАНСЛЯЦІЯ НАЗОВНІ)
    // ============================================================

    /**
     * 📡 Запуск ретрансляції (RTP Forward) аудіоміксу кімнати на вказану IP-адресу та UDP-порт.
     *
     * @param roomId ID кімнати
     * @param host IP отримувача
     * @param rtpPort Порт отримувача
     * @return stream_id для подальшої зупинки
     */
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
            body.put("ssrc", roomId * 1000); // Генеруємо унікальний SSRC для трансляції

            request.put("body", body);

            Map<String, Object> response = sendAndReceive(request);
            validatePluginResponse(response);

            Map<String, Object> plugindata = (Map<String, Object>) response.get("plugindata");
            Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
            
            return ((Number) data.get("stream_id")).longValue();
        });
    }

    /**
     * ⏹️ Зупинка ретрансляції (RTP Forward).
     */
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

    /**
     * 🔍 Отримання списку всіх кімнат та їхніх поточних учасників.
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
            if (plugindata == null) return List.<Map<String, Object>>of();

            Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
            if (data == null) return List.<Map<String, Object>>of();

            List<Map<String, Object>> rooms = (List<Map<String, Object>>) data.get("list");
            if (rooms == null) return List.<Map<String, Object>>of();

            // Для кожної знайденої кімнати дозавантажуємо список учасників
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
     * 🔍 Отримання списку числових ідентифікаторів усіх кімнат у Janus.
     */
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

    /**
     * 👥 Перевіряє, чи присутній користувач із заданим токеном (UUID) у кімнаті.
     */
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

    // ============================================================
    // МОДЕРАЦІЯ
    // ============================================================

    /**
     * 🔇 Виселення (kick) користувача за його токеном (UUID).
     */
    @SuppressWarnings("unchecked")
    public void kickUserFromAudioBridgeRoom(Long roomId, String userToken) {
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

    /**
     * 🔇 Керування мікрофоном учасника (mute/unmute) модератором.
     *
     * 🌀 Auto-Retry:
     * Спочатку робить запит із таємним паролем кімнати (для динамічних кімнат).
     * Якщо Janus відмовляє (наприклад, для статичної кімнати 802, яка не має пароля),
     * бекенд ловить виключення, видаляє параметр "secret" та надсилає команду повторно.
     */
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
            body.put("secret", apiSecret); // спроба з секретом за замовчуванням

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

    /**
     * ❌ Вигнання учасника з кімнати за його внутрішнім ID в Janus.
     *
     * 🌀 Auto-Retry:
     * Аналогічно мутуванню, при невдачі з секретом надсилає запит повторно без параметра "secret".
     */
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
            body.put("secret", apiSecret); // спроба з секретом за замовчуванням

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

    /**
     * 🔎 Діагностичний запит ping до Janus Gateway Admin API.
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
            return "pong".equals(response.get("janus"));
        } catch (Exception e) {
            log.error("Помилка ping до Janus Gateway: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 🔑 Реєстрація токена авторизації користувача в списку дозволених токенів Janus.
     */
    public void registerUserToken(String token) {
        log.info("Реєстрація токена користувача в Janus: {}", token);
        String transactionId = "token-add-" + generateShortUUID();
        
        Map<String, Object> request = Map.of(
                "janus", "add_token",
                "token", token,
                "admin_secret", adminSecret,
                "transaction", transactionId
        );

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

    /**
     * 🔑 Перевірка, чи зареєстрований токен користувача в Janus.
     */
    @SuppressWarnings("unchecked")
    public boolean checkUserTokenInJanus(String token) {
        String transactionId = "tokens-list-" + generateShortUUID();
        Map<String, Object> request = Map.of(
                "janus", "list_tokens",
                "admin_secret", adminSecret,
                "transaction", transactionId
        );

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

    /**
     * Створення базового повідомлення для Client API Janus.
     */
    private Map<String, Object> createBaseRequest(String janusType, String transactionId) {
        Map<String, Object> request = new HashMap<>();
        request.put("janus", janusType);
        request.put("transaction", transactionId);
        request.put("apisecret", apiSecret);
        return request;
    }

    /**
     * Перевірка статусу відповіді Janus.
     * Закидає RuntimeException якщо Janus повернув помилку на транспортному рівні
     * або плагін AudioBridge повернув помилку конфігурації.
     */
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
     * Синхронна відправка запиту у чергу RabbitMQ 'janus-commands'.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> sendAndReceive(Map<String, Object> request) {
        Object res = rabbitTemplate.convertSendAndReceive(request);
        if (res == null) throw new RuntimeException("Таймаут Janus API");
        return (Map<String, Object>) res;
    }

    /**
     * Синхронна відправка адміністративного запиту у чергу RabbitMQ 'janus-admin-commands'.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> sendAndReceiveAdmin(Map<String, Object> request) {
        Object res = adminRabbitTemplate.convertSendAndReceive(request);
        if (res == null) throw new RuntimeException("Таймаут Janus Admin API");
        return (Map<String, Object>) res;
    }

    /**
     * Генерація випадкового унікального ідентифікатора транзакції.
     */
    private String generateShortUUID() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Допоміжний функціональний інтерфейс для дій у сесії.
     */
    @FunctionalInterface
    private interface JanusAction<T> {
        T apply(Long sessionId, Long handleId);
    }

    /**
     * ⚙️ Допоміжний метод виконання дій у контексті тимчасової сесії та хендлу плагіна Janus.
     * Автоматично створює сесію та хендл плагіна, виконує задану функцію `action`,
     * а у блоці `finally` гарантує видалення сесії та звільнення ресурсів Janus.
     */
    private <T> T executeInSession(JanusAction<T> action) {
        Long sessionId = createSession();
        try {
            Long handleId = attachPlugin(sessionId, "janus.plugin.audiobridge");
            return action.apply(sessionId, handleId);
        } finally {
            destroySession(sessionId);
        }
    }

    /**
     * Створення хендлу (handle) підключення плагіна Janus.
     */
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

    /**
     * Створення нової сесії в Janus Gateway.
     */
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

    /**
     * Знищення сесії в Janus Gateway.
     */
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

    /**
     * Надіслати keepalive для підтримки сесії відкритою.
     */
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