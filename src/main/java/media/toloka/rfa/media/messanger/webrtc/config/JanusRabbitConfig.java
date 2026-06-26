package media.toloka.rfa.media.messanger.webrtc.config;

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
 * Повернено до робочої конфігурації v1 з SimpleMessageListenerContainer.
 * Ця конфігурація перевірено працює з Janus.
 */
@Configuration
public class JanusRabbitConfig {

    @Value("${rabbitmq.queueJanus:janus-commands}")
    private String queueJanus;

    @Value("${rabbitmq.queueJanusAdmin:janus-admin-commands}")
    private String queueJanusAdmin;

    @Value("${rabbitmq.queueJanusFrom:from-janus}")
    private String queueJanusFrom;

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

    @Value("${janus.rabbitmq.vhost:/rfa-dev}")
    private String janusVhost;

    private final Gson gson;

    public JanusRabbitConfig(Gson gson) {
        this.gson = gson;
    }

    /**
     * Виділене підключення до RabbitMQ для Janus.
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
     * Конвертер для автоматичного перетворення Java-об'єктів у JSON.
     */
    @Bean(name = "janusMessageConverter")
    public MessageConverter janusMessageConverter() {
        return new GsonAmqpMessageConverter(gson);
    }

    /**
     * Шаблон для Client API.
     */
    @Bean(name = "janusRabbitTemplate")
    public RabbitTemplate janusRabbitTemplate(@Qualifier("janusConnectionFactory") ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setRoutingKey(queueJanus);
        rabbitTemplate.setReplyAddress(queueJanusFrom);
        rabbitTemplate.setReplyTimeout(replyTimeout);
        rabbitTemplate.setMessageConverter(janusMessageConverter());
        return rabbitTemplate;
    }

    /**
     * Шаблон для Admin API.
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
     */
    @Bean
    public SimpleMessageListenerContainer janusReplyListenerContainer(
            @Qualifier("janusConnectionFactory") ConnectionFactory connectionFactory,
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
    public SimpleMessageListenerContainer janusAdminReplyListenerContainer(
            @Qualifier("janusConnectionFactory") ConnectionFactory connectionFactory,
            @Qualifier("janusAdminRabbitTemplate") RabbitTemplate rabbitTemplate) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueues(new Queue(queueJanusAdminFrom));
        container.setMessageListener(rabbitTemplate);
        return container;
    }

    // --- ОГОЛОШЕННЯ ЧЕРГ ---

    @Bean
    public Queue myQueueJanus() {
        return new Queue(queueJanus, true, false, false);
    }

    @Bean
    public Queue myQueueJanusAdmin() {
        return new Queue(queueJanusAdmin, true, false, false);
    }
}