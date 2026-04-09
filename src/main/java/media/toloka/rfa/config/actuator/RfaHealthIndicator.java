package media.toloka.rfa.config.actuator;

import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.tetegrambot.TelegramBot;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Custom health indicator for RFA application.
 * Checks: Database, Telegram Bot, RabbitMQ
 */
@Component
public class RfaHealthIndicator implements HealthIndicator {

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private TelegramBot telegramBot;

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    @Autowired(required = false)
    private ClientService clientService;

    @Override
    public Health health() {
        Health.Builder builder = Health.up();

        // Database health check
        if (dataSource != null) {
            builder.withDetail("database", checkDatabase());
        }

        // Telegram Bot health check
        if (telegramBot != null) {
            builder.withDetail("telegramBot", checkTelegramBot());
        }

        // RabbitMQ health check
        if (rabbitTemplate != null) {
            builder.withDetail("rabbitMQ", checkRabbitMQ());
        }

        // Application info
        builder.withDetail("application", getApplicationInfo());

        return builder.build();
    }

    private Object checkDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            return Health.up()
                    .withDetail("url", conn.getMetaData().getURL())
                    .withDetail("database", conn.getMetaData().getDatabaseProductName())
                    .withDetail("version", conn.getMetaData().getDatabaseProductVersion())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    private Object checkTelegramBot() {
        try {
            return Health.up()
                    .withDetail("status", "configured")
                    .withDetail("client", telegramBot.getTelegramClient() != null ? "ok" : "null")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    private Object checkRabbitMQ() {
        try {
            rabbitTemplate.execute(channel -> {
                channel.getConnection().getServerProperties();
                return null;
            });
            return Health.up()
                    .withDetail("exchange", rabbitTemplate.getExchange())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    private Object getApplicationInfo() {
        return Health.up()
                .withDetail("name", "Radio For All (RFA)")
                .withDetail("version", "1.0.0")
                .withDetail("currentTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }
}
