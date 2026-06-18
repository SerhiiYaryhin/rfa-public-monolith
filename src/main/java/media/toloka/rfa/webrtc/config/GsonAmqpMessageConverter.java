package media.toloka.rfa.webrtc.config;

import com.google.gson.Gson;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;

import java.nio.charset.StandardCharsets;

/**
 * Кастомний конвертер повідомлень для RabbitMQ на базі бібліотеки Google Gson.
 * 
 * Необхідний для коректної серіалізації та десеріалізації повідомлень Janus, 
 * оскільки Janus використовує специфічну структуру JSON, яку стандартні конвертери
 * Spring (Jackson) іноді обробляють некоректно у поєднанні з RabbitMQ RPC.
 */
public class GsonAmqpMessageConverter implements MessageConverter {

    private final Gson gson;

    public GsonAmqpMessageConverter(Gson gson) {
        this.gson = gson;
    }

    /**
     * Конвертація з Java-об'єкта (Map) у бінарне повідомлення для RabbitMQ (відправка).
     * Встановлює правильні заголовки JSON та кодування UTF-8.
     */
    @Override
    public Message toMessage(Object object, MessageProperties messageProperties) throws MessageConversionException {
        String json = this.gson.toJson(object);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setContentEncoding(StandardCharsets.UTF_8.name());
        messageProperties.setContentLength(bytes.length);

        return new Message(bytes, messageProperties);
    }

    /**
     * Конвертація з повідомлення RabbitMQ назад у Java-об'єкт (прийом відповідей).
     * Розпаковує JSON у універсальну структуру Map<String, Object>.
     */
    @Override
    public Object fromMessage(Message message) throws MessageConversionException {
        String json = new String(message.getBody(), StandardCharsets.UTF_8);
        // Розпаковуємо як універсальну карту (LinkedTreeMap у Gson)
        return this.gson.fromJson(json, Object.class);
    }
}
