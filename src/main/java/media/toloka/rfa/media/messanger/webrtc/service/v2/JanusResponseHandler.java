package media.toloka.rfa.media.messanger.webrtc.service.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Асинхронний обробник відповідей від Janus через RabbitMQ.
 *
 * Проблема: Janus не повертає стандартні AMQP correlation headers.
 * Рішення: Корелюємо запити та відповіді за полем "transaction" в JSON.
 */
@Component
public class JanusResponseHandler {

    private static final Logger log = LoggerFactory.getLogger(JanusResponseHandler.class);

    // Map для зберігання очікуваних відповідей: transactionId -> CompletableFuture
    private final Map<String, CompletableFuture<Map<String, Object>>> pendingResponses = new ConcurrentHashMap<>();

    /**
     * Слухає чергу from-janus та корелює відповіді за transaction ID.
     * Ігнорує ACK повідомлення (janus="ack").
     */
    @RabbitListener(queues = "from-janus", concurrency = "1-3")
    public void handleJanusResponse(Map<String, Object> response) {
        String transactionId = (String) response.get("transaction");
        String janusType = (String) response.get("janus");

        log.debug("Отримано відповідь від Janus: type={}, transaction={}", janusType, transactionId);

        // Ігноруємо ACK повідомлення
        if ("ack".equals(janusType)) {
            log.debug("Ігноруємо ACK для transaction={}", transactionId);
            return;
        }

        // Знаходимо очікуючий CompletableFuture
        CompletableFuture<Map<String, Object>> future = pendingResponses.get(transactionId);
        if (future != null) {
            future.complete(response);
            pendingResponses.remove(transactionId);
            log.debug("Відповідь доставлена для transaction={}", transactionId);
        } else {
            log.warn("Отримано відповідь для невідомого transaction={}", transactionId);
        }
    }

    /**
     * Слухає чергу from-janus-admin для Admin API відповідей.
     */
    @RabbitListener(queues = "from-janus-admin", concurrency = "1-3")
    public void handleJanusAdminResponse(Map<String, Object> response) {
        String transactionId = (String) response.get("transaction");
        String janusType = (String) response.get("janus");

        log.debug("Отримано admin відповідь від Janus: type={}, transaction={}", janusType, transactionId);

        // Ігноруємо ACK
        if ("ack".equals(janusType)) {
            log.debug("Ігноруємо admin ACK для transaction={}", transactionId);
            return;
        }

        CompletableFuture<Map<String, Object>> future = pendingResponses.get(transactionId);
        if (future != null) {
            future.complete(response);
            pendingResponses.remove(transactionId);
            log.debug("Admin відповідь доставлена для transaction={}", transactionId);
        } else {
            log.warn("Отримано admin відповідь для невідомого transaction={}", transactionId);
        }
    }

    /**
     * Реєструє очікування відповіді для конкретного transaction ID.
     */
    public CompletableFuture<Map<String, Object>> registerPendingResponse(String transactionId) {
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        pendingResponses.put(transactionId, future);
        log.debug("Зареєстровано очікування відповіді для transaction={}", transactionId);
        return future;
    }

    /**
     * Чекає на відповідь з таймаутом.
     */
    public Map<String, Object> waitForResponse(String transactionId, long timeoutMs) throws Exception {
        CompletableFuture<Map<String, Object>> future = pendingResponses.get(transactionId);
        if (future == null) {
            throw new RuntimeException("Не знайдено очікувану відповідь для transaction=" + transactionId);
        }

        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            pendingResponses.remove(transactionId);
            throw new RuntimeException("Таймаут очікування відповіді для transaction=" + transactionId, e);
        }
    }

    /**
     * Видаляє зареєстровану очікувану відповідь.
     */
    public void removePendingResponse(String transactionId) {
        pendingResponses.remove(transactionId);
    }
}