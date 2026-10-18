package media.toloka.rfa.media.webrtc.config;

import com.google.gson.Gson;
import media.toloka.rfa.media.messanger.webrtc.service.v2.JanusAmqpClient;
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
 * ⚙️ Конфігурація RabbitMQ для взаємодії з медіасервером Janus (версія v2).
 * Використовує SimpleMessageListenerContainer для перехоплення відповідей
 * зі спеціалізованих черг (from-janus / from-janus-admin).
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

    @Value("${janus.rabbitmq.vhost:/rfa}")
    private String janusVhost;

    private final Gson gson;

    public JanusRabbitConfig(Gson gson) {
        this.gson = gson;
    }

    @Bean(name = "janusConnectionFactory")
    public ConnectionFactory janusConnectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbitHost, rabbitPort);
        connectionFactory.setUsername(rabbitUsername);
        connectionFactory.setPassword(rabbitPassword);
        connectionFactory.setVirtualHost(janusVhost);
        return connectionFactory;
    }

    @Bean(name = "janusMessageConverter")
    public MessageConverter janusMessageConverter() {
        return new GsonAmqpMessageConverter(gson);
    }

    @Bean(name = "janusRabbitTemplate")
    public RabbitTemplate janusRabbitTemplate(@Qualifier("janusConnectionFactory") ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setRoutingKey(queueJanus);
        rabbitTemplate.setReplyTimeout(replyTimeout);
        rabbitTemplate.setMessageConverter(janusMessageConverter());
        // Вимикаємо Direct Reply-To, щоб уникнути конфлікту з SimpleMessageListenerContainer
        rabbitTemplate.setUseDirectReplyToContainer(false);
        return rabbitTemplate;
    }

    @Bean(name = "janusAdminRabbitTemplate")
    public RabbitTemplate janusAdminRabbitTemplate(@Qualifier("janusConnectionFactory") ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setRoutingKey(queueJanusAdmin);
        rabbitTemplate.setReplyTimeout(replyTimeout);
        rabbitTemplate.setMessageConverter(janusMessageConverter());
        // Вимикаємо Direct Reply-To
        rabbitTemplate.setUseDirectReplyToContainer(false);
        return rabbitTemplate;
    }

    // ========================================================================
    // 🔑 Слухачі відповідей (Reply Listeners)
    // ========================================================================
    // Реєструємо JanusAmqpClient як MessageListener для контейнерів

    @Bean
    public SimpleMessageListenerContainer janusReplyListenerContainer(
            @Qualifier("janusConnectionFactory") ConnectionFactory connectionFactory,
            JanusAmqpClient janusAmqpClient) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueues(new Queue(queueJanusFrom, true, false, false));
        container.setMessageListener(janusAmqpClient);
        return container;
    }

    @Bean
    public SimpleMessageListenerContainer janusAdminReplyListenerContainer(
            @Qualifier("janusConnectionFactory") ConnectionFactory connectionFactory,
            JanusAmqpClient janusAmqpClient) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueues(new Queue(queueJanusAdminFrom, true, false, false));
        container.setMessageListener(janusAmqpClient);
        return container;
    }
    // ========================================================================

    @Bean
    public Queue myQueueJanus() {
        return new Queue(queueJanus, true, false, false);
    }

    @Bean
    public Queue myQueueJanusAdmin() {
        return new Queue(queueJanusAdmin, true, false, false);
    }
}