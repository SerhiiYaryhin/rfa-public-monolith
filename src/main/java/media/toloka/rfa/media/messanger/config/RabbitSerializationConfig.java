package media.toloka.rfa.media.messanger.config;

import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class RabbitSerializationConfig {

    @Bean
    public SimpleMessageConverter toggleWhiteListMessageConverter() {
        SimpleMessageConverter converter = new SimpleMessageConverter();
        // Явно зашиваємо дозволені пакети на рівні коду біна
        converter.setAllowedListPatterns(List.of(
                "media.toloka.rfa.media.messanger.model.*",
                "media.toloka.rfa.*",
                "java.util.*",
                "java.lang.*"

//                "java.lang.*",
//                "java.util.*",
//                "media.toloka.rfa.media.messanger.model.*"
        ));
        return converter;
    }
}