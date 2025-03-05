package media.toloka.rfa.rpc.config;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableRabbit
@Configuration
public class RPCConfig {

    @Value("${rabbitmq.queue}")
    private String queueName;
    @Value("${rabbitmq.queueTTS}")
    private String queueTTS;
    @Value("${rabbitmq.exchange}")
    private String exchange;
    @Value("${rabbitmq.routingkey}")
    private String routingkey;
    @Value("${rabbitmq.username}")
    private String username;
    @Value("${rabbitmq.password}")
    private String password;
    @Value("${rabbitmq.host}")
    private String host;
    @Value("${rabbitmq.virtualhost}")
//    @Value("${QRFA}")
//    @Value("${QRFA}")
    private String virtualHost;
    @Value("${rabbitmq.reply.timeout}")
    private Integer replyTimeout;
    @Value("${rabbitmq.concurrent.consumers}")
    private Integer concurrentConsumers;
    @Value("${rabbitmq.max.concurrent.consumers}")
    private Integer maxConcurrentConsumers;

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        // todo QRFA довести до ладу використання змінних середовища
        String vhost = System.getenv("QRFA");
        if (vhost != null) {
            connectionFactory.setVirtualHost(vhost);
            System.out.println("======================== QRFA = "+vhost);
        } else {
            connectionFactory.setVirtualHost(virtualHost);
            System.out.println("======================== virtualHost = "+virtualHost);
        }
        return connectionFactory;
    }

    @Bean
    public AmqpAdmin amqpAdmin() {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory());
        return rabbitAdmin;
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
        rabbitTemplate.setDefaultReceiveQueue(queueName);
        rabbitTemplate.setReplyTimeout(replyTimeout);
        //no reply to - we use direct-reply-to
        return rabbitTemplate;
    }

//    @Bean
//    public RabbitTemplate rabbitTTSTemplate() {
//        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
//        rabbitTemplate.setDefaultReceiveQueue(queueTTS);
//        rabbitTemplate.setReplyTimeout(replyTimeout);
//        //no reply to - we use direct-reply-to
//        return rabbitTemplate;
//    }


    @Bean
    public Queue myQueue() {
        return new Queue(queueName);
    }

    @Bean
    public Queue myQueueTTS() {
        return new Queue(queueTTS);
    }

}
