package media.toloka.rfa.webrtc.service;

import com.google.gson.Gson;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RPC-клієнт для взаємодії з медіасервером Janus через брокер повідомлень RabbitMQ.
 * Використовує патерн Request-Reply (Direct Reply-To) для синхронного очікування відповідей.
 * Усі JSON-повідомлення автоматично серіалізуються та десеріалізуються через кастомний GsonMessageConverter.
 */
@Service
public class JanusRabbitClient {

    // Спеціальний екземпляр RabbitTemplate, налаштований суто під тайм-аути та черги Janus
    private final RabbitTemplate rabbitTemplate;

    // Назва черги, яку слухає Janus (береться з application-chat.properties)
    private final String janusQueue;

    // Глобальний секретний ключ адміністратора Janus (збігається з admin_secret в janus.jcfg)
    @Value("${janus.admin_secret}")
    private String adminSecret;

    /**
     * Конструктор для впровадження залежностей (Dependency Injection).
     * Використовує @Qualifier, щоб точно інжектувати шаблон, створений у JanusRabbitConfig,
     * а не загальний шаблон для керування радіостанціями.
     */
    public JanusRabbitClient(@Qualifier("janusRabbitTemplate") RabbitTemplate rabbitTemplate,
                             @Value("${rabbitmq.queueJanus:janus-commands}") String janusQueue) {
        this.rabbitTemplate = rabbitTemplate;
        this.janusQueue = janusQueue;
    }

    /**
     * 1. Динамічне створення кімнати голосового чату (плагін AudioBridge) перед початком ефіру.
     *
     * @param roomDescription Текстовий опис кімнати (наприклад, назва передачі чи радіостанції)
     * @return Унікальний числовий ID створеної кімнати, який згенерував Janus
     */
    public Long createDynamicRoom(String roomDescription) {
        // Формуємо транзакційний ID для відстеження цього конкретного запиту
        String transactionId = "room-create-" + generateShortUUID();

        // Будуємо структуру запиту відповідно до документації Janus AudioBridge API
        Map<String, Object> request = Map.of(
                "janus", "message",                                 // Тип запиту (текстове повідомлення до плагіну)
                "admin_secret", adminSecret,                        // Токен супер-адміна для валідації права створення
                "transaction", transactionId,                       // Ідентифікатор транзакції
                "body", Map.of(
                        "request", "create",                            // Команда плагіну: створити кімнату
                        "description", roomDescription,                 // Опис для метаданих
                        "sampling", 48000,                              // Частота дискретизації (48kHz Opus — стандарт WebRTC)
                        "is_private", false                             // Кімната відкрита для внутрішніх підключень за токенами
                )
        );

        // Відправляємо в чергу RabbitMQ і чекаємо на відповідь (синхронний RPC-виклик)
        Map<String, Object> response = sendAndReceive(request);

        // Розбираємо JSON-відповідь за деревом: response -> data -> room
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data == null || !data.containsKey("room")) {
            throw new IllegalStateException("Janus не повернув ID створеної кімнати. Відповідь: " + response);
        }

        // Увага: Gson за замовчуванням мапить усі числа без явного типу в Double.
        // Тому спочатку кастимо в Double, а потім безпечно забираємо Long значення.
        Double roomID = (Double) data.get("room");
        return roomID.longValue();
    }

    /**
     * 2. Реєстрація унікального безпечного токена доступу для слухача в Janus.
     * Без цього токена фронтенд не зможе підняти WebRTC-стрим із медіасервером.
     *
     * @param userToken Унікальний токен, згенерований Spring Boot для конкретного юзера
     */
    public void registerUserToken(String userToken) {
        String transactionId = "token-reg-" + generateShortUUID();

        // Будуємо глобальну команду для керування токенами в Janus Core
        Map<String, Object> request = Map.of(
                "janus", "add_token",                               // Команда Янусу: додати новий дозволений токен
                "token", userToken,                                 // Сам рядок токена (передамо клієнту на фронт)
                "admin_secret", adminSecret,                        // Секрет адміна
                "transaction", transactionId,                       // Ідентифікатор транзакції
                "plugins", List.of("janus.plugin.audiobridge")      // Список плагінів, до яких цей токен дає доступ
        );

        Map<String, Object> response = sendAndReceive(request);

        // Перевіряємо, чи успішно Janus виконав глобальну адмін-команду
        String status = (String) response.get("janus");
        if (!"success".equals(status)) {
            throw new RuntimeException("Не вдалося зареєструвати токен у Janus. Відповідь: " + response);
        }
    }

    /**
     * 3. Керування мікрофоном користувача на рівні сервера (Mute / Unmute).
     * Використовується модератором для виведення слухача в прямий ефір або повернення в чергу.
     *
     * @param roomId             ID динамічної кімнати (отриманий раніше з бази даних)
     * @param participantJanusId Внутрішній ID учасника, який Janus присвоїв йому при вході в кімнату
     * @param mute               true — вимкнути мікрофон (черга), false — увімкнути (прямий ефір)
     */
    public void toggleMute(Long roomId, Long participantJanusId, boolean mute) {
        String transactionId = (mute ? "mute-" : "unmute-") + generateShortUUID();

        // Конфігураційний запит для плагіна всередині конкретної кімнати
        Map<String, Object> request = Map.of(
                "janus", "message",
                "admin_secret", adminSecret,
                "transaction", transactionId,
                "body", Map.of(
                        "request", "configure",                        // Команда: змінити конфігурацію учасника
                        "room", roomId,                                 // Динамічний ID кімнати
                        "id", participantJanusId,                       // ID сесії конкретного слухача
                        "muted", mute                                   // Новий стан мікрофона
                )
        );

        Map<String, Object> response = sendAndReceive(request);

        // Валідація відповіді від плагіна AudioBridge
        Map<String, Object> plugindata = (Map<String, Object>) response.get("plugindata");
        if (plugindata != null) {
            Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
            if (data != null && data.containsKey("error")) {
                throw new RuntimeException("Janus AudioBridge повернув помилку: " + data.get("error"));
            }
        }
    }

    /**
     * Внутрішній допоміжний метод для здійснення RPC-виклику через RabbitMQ.
     * Ізолює логіку взаємодії з RabbitTemplate та кастинг типів respuestas.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> sendAndReceive(Map<String, Object> request) {
        try {
            // Spring AMQP під капотом підставить 'amq.rabbitmq.reply-to' як reply_to заголовок,
            // надішле повідомлення, заблокує потік на час таймауту і поверне десеріалізований через Gson об'єкт.
            Object rawResponse = rabbitTemplate.convertSendAndReceive(janusQueue, request);

            if (rawResponse == null) {
                throw new RuntimeException("Таймаут відповіді від Janus RPC через RabbitMQ (сервер не відповів за встановлений час)");
            }

            return (Map<String, Object>) rawResponse;
        } catch (ClassCastException e) {
            throw new RuntimeException("Помилка кастингу відповіді Janus. Очікувався Map, але отримано інший тип.", e);
        }
    }

    /**
     * Генератор коротких унікальних ідентифікаторів для транзакцій (довжиною 8 символів).
     * Зменшує розмір логів і повідомлень у черзі, зберігаючи унікальність у межах сесії.
     */
    private String generateShortUUID() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Динамічне створення кімнати голосового чату із виділеним RTP-портом під ефір радіо.
     *
     * @param roomDescription Опис кімнати (наприклад, "Ефір Толока Радіо")
     * @param rtpPort          Унікальний UDP-порт на сервері Janus, куди станція буде літи потік (наприклад, 10002)
     * @return ID створеної кімнати
     */
    public Long createDynamicRoomWithRadio(String roomDescription, Integer rtpPort) {
        String transactionId = "room-rtp-create-" + generateShortUUID();

        Map<String, Object> request = Map.of(
                "janus", "message",
                "admin_secret", adminSecret,
                "transaction", transactionId,
                "body", Map.of(
                        "request", "create",
                        "description", roomDescription,
                        "sampling", 48000,                  // Стандарт для WebRTC Opus
                        "is_private", false,

                        // --- НАЛАШТУВАННЯ ДЛЯ РАДІОПОТОКУ ---
                        "rtp_stream", true,                 // Вмикаємо підтримку зовнішнього RTP стриму
                        "rtp_stream_port", rtpPort,         // Порт, який Janus відкриє для прийому UDP звуку
                        "rtp_stream_codec", "opus",         // Кодек трансляції
                        "rtp_stream_sample_rate", 48000,    // Частота дискретизації ефіру
                        "rtp_stream_channels", 2            // Стерео для якісного звуку радіо
                )
        );

        Map<String, Object> response = sendAndReceive(request);

        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data == null || !data.containsKey("room")) {
            throw new IllegalStateException("Janus не повернув ID створеної RTP-кімнати. Відповідь: " + response);
        }

        Double roomID = (Double) data.get("room");
        return roomID.longValue();
    }
}