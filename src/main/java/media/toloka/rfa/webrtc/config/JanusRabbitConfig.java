package media.toloka.rfa.webrtc.config;

import com.google.gson.Gson;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфігурація RabbitMQ для взаємодії з медіасервером Janus.
 * 
 * Цей клас налаштовує два незалежні канали зв'язку:
 * 1. Стандартний API (Client API) — для сесій, хендлів та повідомлень плагінам.
 * 2. Адміністративний API (Admin API) — для глобальних налаштувань та токенів.
 * 
 * Особливістю є використання окремого ConnectionFactory з власним VHost (/rfa-dev),
 * щоб ізолювати трафік Janus від основної логіки порталу.
 */
@Configuration
public class JanusRabbitConfig {

    // Черга для відправки команд клієнтського API
    @Value("${rabbitmq.queueJanus:janus-commands}")
    private String queueJanus;

    // Черга для відправки команд адміністративного API
    @Value("${rabbitmq.queueJanusAdmin:janus-admin-commands}")
    private String queueJanusAdmin;

    // Черга, де Janus залишає відповіді для клієнтського API
    @Value("${rabbitmq.queueJanusFrom:from-janus}")
    private String queueJanusFrom;

    // Черга, де Janus залишає відповіді для адміністративного API
    @Value("${rabbitmq.queueJanusAdminFrom:from-janus-admin}")
    private String queueJanusAdminFrom;

    @Value("${rabbitmq.reply.timeout:5000}")
    private Integer replyTimeout;

    @Value("${rabbitmq.host}")
    private String rabbitHost;

    @Value("${rabbitmq.port:5672}")
    private int rabbitPort;

    @Value("${rabbitmq.username}")
    private String rabbitUsername;

    @Value("${rabbitmq.password}")
    private String rabbitPassword;

    // Спеціальний віртуальний хост для Janus
    @Value("${janus.rabbitmq.vhost:/rfa-dev}")
    private String janusVhost;

    private final Gson gson;

    public JanusRabbitConfig(Gson gson) {
        this.gson = gson;
    }

    /**
     * Створює виділене підключення до RabbitMQ для Janus.
     * Використовує окремий VHost для запобігання конфліктам черг.
     */
    @Bean(name = "janusConnectionFactory")
    public ConnectionFactory janusConnectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbitHost, rabbitPort);
        connectionFactory.setUsername(rabbitUsername);
        connectionFactory.setPassword(rabbitPassword);
        connectionFactory.setVirtualHost(janusVhost);
        return connectionFactory;
    }

    /**
     * Конвертер для автоматичного перетворення Java-об'єктів (Map) у JSON-повідомлення Janus.
     */
    @Bean(name = "janusMessageConverter")
    public MessageConverter janusMessageConverter() {
        return new GsonAmqpMessageConverter(gson);
    }

    /**
     * Налаштування шаблону для Client API.
     * Використовує фіксовану чергу для відповідей (Reply Address).
     */
    @Bean(name = "janusRabbitTemplate")
    public RabbitTemplate janusRabbitTemplate(@Qualifier("janusConnectionFactory") ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setRoutingKey(queueJanus);
        rabbitTemplate.setReplyAddress(queueJanusFrom); // Вказуємо, де чекати відповідь
        rabbitTemplate.setReplyTimeout(replyTimeout);
        rabbitTemplate.setMessageConverter(janusMessageConverter());
        return rabbitTemplate;
    }

    /**
     * Налаштування шаблону для Admin API.
     */
    @Bean(name = "janusAdminRabbitTemplate")
    public RabbitTemplate janusAdminRabbitTemplate(@Qualifier("janusConnectionFactory") ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setRoutingKey(queueJanusAdmin);
        rabbitTemplate.setReplyAddress(queueJanusAdminFrom);
        rabbitTemplate.setReplyTimeout(replyTimeout);
        rabbitTemplate.setMessageConverter(janusMessageConverter());
        return rabbitTemplate;
    }

    /**
     * Контейнер для прослуховування черги відповідей Client API.
     * Необхідний для реалізації патерну Request-Response через статичні черги.
     */
    @Bean
    public SimpleMessageListenerContainer janusReplyListenerContainer(@Qualifier("janusConnectionFactory") ConnectionFactory connectionFactory,
                                                                      @Qualifier("janusRabbitTemplate") RabbitTemplate rabbitTemplate) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueues(new Queue(queueJanusFrom));
        container.setMessageListener(rabbitTemplate);
        return container;
    }

    /**
     * Контейнер для прослуховування черги відповідей Admin API.
     */
    @Bean
    public SimpleMessageListenerContainer janusAdminReplyListenerContainer(@Qualifier("janusConnectionFactory") ConnectionFactory connectionFactory,
                                                                           @Qualifier("janusAdminRabbitTemplate") RabbitTemplate rabbitTemplate) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueues(new Queue(queueJanusAdminFrom));
        container.setMessageListener(rabbitTemplate);
        return container;
    }

    // --- ОГОЛОШЕННЯ ЧЕРГ (щоб Spring створив їх автоматично, якщо вони відсутні) ---

    @Bean
    public Queue myQueueJanus() {
        return new Queue(queueJanus, true, false, false);
    }

    @Bean
    public Queue myQueueJanusAdmin() {
        return new Queue(queueJanusAdmin, true, false, false);
    }
}
