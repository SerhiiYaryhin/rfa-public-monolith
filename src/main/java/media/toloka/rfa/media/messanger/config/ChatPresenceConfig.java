package media.toloka.rfa.media.messanger.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфігурація RabbitMQ для відстеження присутності користувачів у чаті.
 */
@Configuration
public class ChatPresenceConfig {

    public static final String CHAT_PRESENCE_EXCHANGE = "chat.presence.exchange";
    public static final String CHAT_PRESENCE_QUEUE_PREFIX = "chat.presence.queue.";

    @Bean
    public FanoutExchange chatPresenceExchange() {
        return new FanoutExchange(CHAT_PRESENCE_EXCHANGE);
    }

    /**
     * Створюємо анонімну автовидаляємо чергу для кожного вузла сервера.
     * Це гарантує, що кожен екземпляр додатка отримає всі події присутності.
     */
    @Bean
    public Queue chatPresenceQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Binding chatPresenceBinding(Queue chatPresenceQueue, FanoutExchange chatPresenceExchange) {
        return BindingBuilder.bind(chatPresenceQueue).to(chatPresenceExchange);
    }
}
