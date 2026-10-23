package media.toloka.rfa.media.messanger.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Конфігурація WebSocket з використанням RabbitMQ STOMP Relay.
 * Забезпечує доставку повідомлень між усіма вузлами кластера.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${rabbitmq.host}")
    private String host;

    @Value("${rabbitmq.username}")
    private String username;

    @Value("${rabbitmq.password}")
    private String password;

    @Value("${rabbitmq.virtualhost}")
    private String virtualHost;

    @Autowired
    private ChatRoomPresenceInterceptor chatRoomPresenceInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Використовуємо RabbitMQ як зовнішній брокер (STOMP Relay)
        config.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost(host)
                .setRelayPort(61613) // Стандартний порт STOMP для RabbitMQ
                .setClientLogin(username)
                .setClientPasscode(password)
                .setSystemLogin(username)
                .setSystemPasscode(password)
                .setVirtualHost(virtualHost);

        // Префікс для повідомлень, які обробляються контролерами (@MessageMapping)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/rfachat")
                .setAllowedOrigins(
                        "https://rfa.toloka.media",
                        "https://test.rfa.toloka.media",
                        "https://localhost",
                        "http://localhost:3080")
                .withSockJS();
    }

    /**
     * ✅ Реєструємо interceptor для відстеження підписок на кімнати.
     * Це дозволяє ChatRoomPresenceService знати, хто в якій кімнаті.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(chatRoomPresenceInterceptor);
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("stomp-relay-task-");
        scheduler.initialize();
        return scheduler;
    }
}