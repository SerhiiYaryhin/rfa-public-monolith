package media.toloka.rfa.webrtc.config;

import com.google.gson.Gson;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JanusRabbitConfig {

    @Value("${rabbitmq.queueJanus:janus-commands}")
    private String queueJanus;

    @Value("${rabbitmq.reply.timeout:5000}")
    private Integer replyTimeout;

    private final ConnectionFactory connectionFactory;
    private final Gson gson;

    public JanusRabbitConfig(ConnectionFactory connectionFactory, Gson gson) {
        this.connectionFactory = connectionFactory;
        this.gson = gson;
    }

    @Bean(name = "janusMessageConverter")
    public MessageConverter janusMessageConverter() {
        // Використовуємо наш власний локальний конвертер
        return new GsonAmqpMessageConverter(gson);
    }

    @Bean(name = "janusRabbitTemplate")
    public RabbitTemplate janusRabbitTemplate() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setRoutingKey(queueJanus);
        rabbitTemplate.setReplyTimeout(replyTimeout);
        rabbitTemplate.setMessageConverter(janusMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public Queue myQueueJanus() {
        return new Queue(queueJanus, true, false, false);
    }
}