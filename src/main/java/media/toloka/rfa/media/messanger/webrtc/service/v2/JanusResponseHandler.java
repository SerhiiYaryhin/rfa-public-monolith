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

    private final Map<String, CompletableFuture<Map<String, Object>>> pendingResponses = new ConcurrentHashMap<>();

    /**
     * ✅ ВИПРАВЛЕНО: Додано containerFactory = "janusListenerContainerFactory"
     * Це гарантує, що MessageConverter конвертує JSON byte[] в Map<String, Object>.
     */
    @RabbitListener(
            queues = "from-janus",
            containerFactory = "janusListenerContainerFactory",
            concurrency = "1-3"
    )
    public void handleJanusResponse(Map<String, Object> response) {
        String transactionId = (String) response.get("transaction");
        String janusType = (String) response.get("janus");

        log.debug("Отримано відповідь від Janus: type={}, transaction={}", janusType, transactionId);

        // Ігноруємо ACK повідомлення
        if ("ack".equals(janusType)) {
            log.debug("Ігноруємо ACK для transaction={}", transactionId);
            return;
        }

        CompletableFuture<Map<String, Object>> future = pendingResponses.get(transactionId);
        if (future != null) {
            future.complete(response);
            pendingResponses.remove(transactionId);
            log.debug("Відповідь доставлена для transaction={}", transactionId);
        } else {
            log.warn("Отримано відповідь для невідомого transaction={}", transactionId);
        }
    }

    @RabbitListener(
            queues = "from-janus-admin",
            containerFactory = "janusListenerContainerFactory",
            concurrency = "1-3"
    )
    public void handleJanusAdminResponse(Map<String, Object> response) {
        String transactionId = (String) response.get("transaction");
        String janusType = (String) response.get("janus");

        log.debug("Отримано admin відповідь від Janus: type={}, transaction={}", janusType, transactionId);

        if ("ack".equals(janusType)) {
            log.debug("Ігноруємо admin ACK для transaction={}", transactionId);
            return;
        }

        CompletableFuture<Map<String, Object>> future = pendingResponses.get(transactionId);
        if (future != null) {
            future.complete(response);
            pendingResponses.remove(transactionId);
            log.debug("Admin відповідь доставлена для transaction={}", transactionId);
        }
    }

    public CompletableFuture<Map<String, Object>> registerPendingResponse(String transactionId) {
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        pendingResponses.put(transactionId, future);
        log.debug("Зареєстровано очікування для transaction={}", transactionId);
        return future;
    }

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

    public void removePendingResponse(String transactionId) {
        pendingResponses.remove(transactionId);
    }
}