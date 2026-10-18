package media.toloka.rfa.media.messanger.webrtc.service.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 🔐 Сервіс адміністрування Janus Gateway.
 * Відповідає за реєстрацію токенів, ping, перевірку токенів.
 */
@Service
public class JanusAdminService {

    private static final Logger log = LoggerFactory.getLogger(JanusAdminService.class);

    private final JanusAmqpClient amqpClient;

    @Value("${janus.admin_secret}")
    private String adminSecret;

    public JanusAdminService(JanusAmqpClient amqpClient) {
        this.amqpClient = amqpClient;
    }

    /**
     * 🔎 Діагностичний запит ping до Janus Gateway Admin API.
     */
    public boolean pingJanus() {
        String transactionId = amqpClient.generateTransactionId("ping");
        Map<String, Object> request = Map.of(
                "janus", "ping",
                "admin_secret", adminSecret,
                "transaction", transactionId
        );

        try {
            Map<String, Object> response = amqpClient.sendAndReceiveAdmin(request);
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

        String transactionId = amqpClient.generateTransactionId("token-add");
        Map<String, Object> request = Map.of(
                "janus", "add_token",
                "token", token,
                "admin_secret", adminSecret,
                "transaction", transactionId
        );

        try {
            Map<String, Object> response = amqpClient.sendAndReceiveAdmin(request);

            if (!"success".equals(response.get("janus"))) {
                throw new RuntimeException("Помилка реєстрації токена: " + response);
            }

            log.info("Токен користувача {} успішно зареєстровано в Janus", token);
        } catch (Exception e) {
            log.error("Помилка реєстрації токена користувача: {}", e.getMessage(), e);
            throw new RuntimeException("Помилка реєстрації токена в Janus", e);
        }
    }

    /**
     * 🔑 Перевірка, чи зареєстрований токен користувача в Janus.
     */
    @SuppressWarnings("unchecked")
    public boolean checkUserTokenInJanus(String token) {
        String transactionId = amqpClient.generateTransactionId("tokens-list");
        Map<String, Object> request = Map.of(
                "janus", "list_tokens",
                "admin_secret", adminSecret,
                "transaction", transactionId
        );

        try {
            Map<String, Object> response = amqpClient.sendAndReceiveAdmin(request);

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
            log.error("Помилка перевірки списку токенів Janus: {}", e.getMessage(), e);
        }

        return false;
    }
}