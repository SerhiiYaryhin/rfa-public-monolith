package media.toloka.rfa.media.messanger.config;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RabbitSerializationConfig {

    @Bean
    public SimpleMessageConverter messageConverter() {
        SimpleMessageConverter converter = new SimpleMessageConverter();
        // Дозволяємо десеріалізацію дозволених пакетів тут
        converter.setAllowedListPatterns(List.of(
                "media.toloka.rfa.media.messanger.model.*",
                "media.toloka.rfa.*",
                "java.util.*",
                "java.lang.*"
        ));
        return converter;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            SimpleMessageConverter messageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);

        // Прив'язуємо налаштований конвертер до фабрики
        factory.setMessageConverter(messageConverter);

        return factory;
    }
}