package media.toloka.rfa.media.messanger.webrtc.config;

import com.google.gson.Gson;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JanusRabbitConfig {

    @Value("${rabbitmq.queueJanus:janus-commands}")
    private String queueJanus;

    @Value("${rabbitmq.queueJanusAdmin:janus-admin-commands}")
    private String queueJanusAdmin;

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

    /**
     * ✅ ВИПРАВЛЕНО: Видалено replyAddress (тепер не використовується).
     * Відповіді обробляються через @RabbitListener в JanusResponseHandler.
     */
    @Bean(name = "janusRabbitTemplate")
    public RabbitTemplate janusRabbitTemplate(@Qualifier("janusConnectionFactory") ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setRoutingKey(queueJanus);
        // ❌ ВИДАЛЕНО: rabbitTemplate.setReplyAddress(queueJanusFrom);
        rabbitTemplate.setReplyTimeout(replyTimeout);
        rabbitTemplate.setMessageConverter(janusMessageConverter());
        return rabbitTemplate;
    }

    @Bean(name = "janusAdminRabbitTemplate")
    public RabbitTemplate janusAdminRabbitTemplate(@Qualifier("janusConnectionFactory") ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setRoutingKey(queueJanusAdmin);
        // ❌ ВИДАЛЕНО: rabbitTemplate.setReplyAddress(queueJanusAdminFrom);
        rabbitTemplate.setReplyTimeout(replyTimeout);
        rabbitTemplate.setMessageConverter(janusMessageConverter());
        return rabbitTemplate;
    }

    // ❌ ВИДАЛЕНО: janusReplyListenerContainer та janusAdminReplyListenerContainer
    // Тепер використовуємо @RabbitListener в JanusResponseHandler

    @Bean
    public Queue myQueueJanus() {
        return new Queue(queueJanus, true, false, false);
    }

    @Bean
    public Queue myQueueJanusAdmin() {
        return new Queue(queueJanusAdmin, true, false, false);
    }

    @Bean
    public Queue fromJanusQueue() {
        return new Queue("from-janus", true, false, false);
    }

    @Bean
    public Queue fromJanusAdminQueue() {
        return new Queue("from-janus-admin", true, false, false);
    }
}