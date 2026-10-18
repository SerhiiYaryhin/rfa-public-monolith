package media.toloka.rfa.media.messanger.webrtc.service.v2;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 *  Низькорівневий AMQP клієнт для взаємодії з Janus Gateway.
 * Відповідає за:
 * - Відправку запитів у черги RabbitMQ
 * - Отримання та кореляцію відповідей (ігнорує проміжні "ack")
 * - Валідацію відповідей Janus API
 * - Генерацію transaction ID
 */
@Component
public class JanusAmqpClient implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(JanusAmqpClient.class);
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private final RabbitTemplate rabbitTemplate;
    private final RabbitTemplate adminRabbitTemplate;
    private final Gson gson;

    @Value("${rabbitmq.reply.timeout:5000}")
    private long replyTimeout;

    // Мапа очікуваних відповідей за transaction ID
    private final Map<String, CompletableFuture<Map<String, Object>>> pendingRequests = new ConcurrentHashMap<>();

    public JanusAmqpClient(
            @Qualifier("janusRabbitTemplate") RabbitTemplate rabbitTemplate,
            @Qualifier("janusAdminRabbitTemplate") RabbitTemplate adminRabbitTemplate,
            Gson gson) {
        this.rabbitTemplate = rabbitTemplate;
        this.adminRabbitTemplate = adminRabbitTemplate;
        this.gson = gson;
    }

    // ============================================================
    // ОБРОБКА ВХІДНИХ ПОВІДОМЛЕНЬ (MessageListener)
    // ============================================================

    @Override
    public void onMessage(Message message) {
        try {
            String json = new String(message.getBody(), "UTF-8");
            Map<String, Object> response = gson.fromJson(json, MAP_TYPE);

            // Отримуємо transaction ID з заголовка або з тіла повідомлення
            String transactionId = message.getMessageProperties().getCorrelationId();
            if (transactionId == null && response.containsKey("transaction")) {
                transactionId = (String) response.get("transaction");
            }

            if (transactionId != null) {
                CompletableFuture<Map<String, Object>> future = pendingRequests.get(transactionId);
                if (future != null) {
                    String janusType = (String) response.get("janus");
                    if ("ack".equals(janusType)) {
                        // Проміжне підтвердження - не завершуємо future
                        log.debug("Отримано ack для транзакції {}", transactionId);
                    } else {
                        // Фінальна відповідь (success або error)
                        pendingRequests.remove(transactionId);
                        future.complete(response);
                    }
                } else {
                    log.debug("Ігноруємо відповідь для завершеної або невідомої транзакції: {}", transactionId);
                }
            }
        } catch (Exception e) {
            log.error("Помилка обробки повідомлення від Janus: {}", e.getMessage(), e);
        }
    }

    // ============================================================
    // МЕТОДИ ВІДПРАВКИ ТА ОЧІКУВАННЯ
    // ============================================================

    /**
     * Синхронна відправка запиту у чергу 'janus-commands' та очікування відповіді.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> sendAndReceive(Map<String, Object> request) {
        String transactionId = (String) request.get("transaction");
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        pendingRequests.put(transactionId, future);

        try {
            rabbitTemplate.convertAndSend(request);
            return future.get(replyTimeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            pendingRequests.remove(transactionId);
            log.error(" Таймаут Janus API для транзакції {}: {}", transactionId, e.getMessage());
            throw new RuntimeException("Таймаут Janus API", e);
        }
    }

    /**
     * Синхронна відправка адміністративного запиту у чергу 'janus-admin-commands'.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> sendAndReceiveAdmin(Map<String, Object> request) {
        String transactionId = (String) request.get("transaction");
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        pendingRequests.put(transactionId, future);

        try {
            adminRabbitTemplate.convertAndSend(request);
            return future.get(replyTimeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            pendingRequests.remove(transactionId);
            log.error("❌ Таймаут Janus Admin API для транзакції {}: {}", transactionId, e.getMessage());
            throw new RuntimeException("Таймаут Janus Admin API", e);
        }
    }

    // ============================================================
    // ДОПОМІЖНІ МЕТОДИ
    // ============================================================

    /**
     * Створення базового повідомлення для Client API Janus.
     */
    public Map<String, Object> createBaseRequest(String janusType, String transactionId, String apiSecret) {
        Map<String, Object> request = new HashMap<>();
        request.put("janus", janusType);
        request.put("transaction", transactionId);
        request.put("apisecret", apiSecret);
        return request;
    }

    /**
     * Перевірка статусу відповіді Janus.
     * Кидає RuntimeException якщо Janus повернув помилку.
     */
    @SuppressWarnings("unchecked")
    public void validatePluginResponse(Map<String, Object> response) {
        // Перевіряємо на явну помилку транспорту Janus
        if ("error".equals(response.get("janus"))) {
            throw new RuntimeException("Janus API error: " + response.get("error") + " (" + response.get("error_code") + ")");
        }

        // Перевіряємо на помилку всередині даних плагіна
        Map<String, Object> plugindata = (Map<String, Object>) response.get("plugindata");
        if (plugindata != null) {
            Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
            if (data != null && (data.containsKey("error") || data.containsKey("error_code"))) {
                throw new RuntimeException("Plugin error: " + (data.get("error") != null ? data.get("error") : data.get("error_code")));
            }
        }
        // Якщо "janus" == "success" або "janus" == "event" і немає ключів "error", вважаємо це успіхом
    }

    /**
     * Генерація випадкового унікального ідентифікатора транзакції.
     */
    public String generateTransactionId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}