package media.toloka.rfa.webrtc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import media.toloka.rfa.webrtc.model.JanusLiveStream;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

/**
 * Високорівневий сервіс для керування прямими ефірами через WebRTC.
 * 
 * Цей сервіс координує роботу декількох систем:
 * 1. Janus (через JanusRabbitClient) — створення медіа-кімнати.
 * 2. Порти (через PortAllocationService) — виділення вільного UDP порту.
 * 3. FFmpeg Агент (через RabbitMQ) — запуск процесу перекодування аудіопотоку.
 */
@Service
public class RadioLiveService {

    private final JanusRabbitClient janusClient;
    private final PortAllocationService portService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public RadioLiveService(JanusRabbitClient janusClient,
                            PortAllocationService portService,
                            RabbitTemplate rabbitTemplate,
                            ObjectMapper objectMapper) {
        this.janusClient = janusClient;
        this.portService = portService;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Запуск трансляції радіостанції в WebRTC ефір.
     * @param stationId ID станції (наприклад, 'rfa-rock')
     * @param sourceStreamUrl URL джерела (наприклад, Icecast потік)
     */
    @Transactional
    public JanusLiveStream startBroadcast(String stationId, String sourceStreamUrl) {
        // Якщо ефір вже запущено — повертаємо існуючий стрім
        return portService.getStreamByStation(stationId).orElseGet(() -> {
            // Шукаємо вільний порт
            Integer freePort = portService.findNextFreePort();

            // 1. Створюємо кімнату в Janus з підтримкою RTP на виділеному порту
            Long janusRoomId = janusClient.createDynamicRoomWithRadio("Ефір: " + stationId, freePort);

            // 2. Надсилаємо команду FFmpeg Агенту для запуску трансляції.
            // FFmpeg почне читати sourceStreamUrl і пересилати його в Opus/RTP на janus.rfa:freePort
            sendFfmpegCommand("start", stationId, sourceStreamUrl, "rtp://janus.rfa:" + freePort);

            // 3. Зберігаємо стан у базу даних
            return portService.saveActiveStream(stationId, janusRoomId, freePort, 0);
        });
    }

    /**
     * Зупинка трансляції.
     * Припиняє роботу FFmpeg та видаляє запис про стрім, звільняючи UDP порт.
     */
    @Transactional
    public void stopBroadcast(String stationId) {
        portService.getStreamByStation(stationId).ifPresent(stream -> {
            // 1. Команда Агенту на зупинку процесу
            sendFfmpegCommand("stop", stationId, null, null);

            // 2. Видаляємо запис з БД
            portService.deleteStream(stationId);
        });
    }

    /**
     * Генерує унікальний токен для користувача та реєструє його в Janus.
     * Це дозволяє фронтенду підключатися без знання статичного api_secret.
     */
    public String generateAndRegisterToken() {
        String token = "rfa-token-" + java.util.UUID.randomUUID().toString().substring(0, 12);
        janusClient.registerUserToken(token);
        return token;
    }

    /**
     * Перевірка зв'язку з Janus Admin API.
     */
    public boolean pingJanus() {
        return janusClient.pingJanus();
    }

    /**
     * Формує та надсилає JSON-команду для FFmpeg Агента через RabbitMQ.
     */
    private void sendFfmpegCommand(String command, String stationId, String streamUrl, String rtpUrl) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("command", command);
            message.put("stationId", stationId);
            if ("start".equals(command)) {
                message.put("streamUrl", streamUrl);
                message.put("rtpUrl", rtpUrl);
            }

            // Серіалізуємо в JSON та відправляємо в чергу
            String jsonPayload = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend("ffmpeg-agent-commands", jsonPayload);

        } catch (Exception e) {
            throw new RuntimeException("Помилка відправки команди FFmpeg: " + e.getMessage());
        }
    }

    /**
     * Отримання повної інформації про активні кімнати Janus, їхніх учасників
     * та статус трансляції (чи підключено Icecast-потік до цієї кімнати).
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getActiveRoomsInfo() {
        List<Map<String, Object>> rooms;
        try {
            rooms = janusClient.getRoomsAndParticipants();
        } catch (Exception e) {
            rooms = Collections.emptyList();
        }

        Map<Long, JanusLiveStream> activeStreamsMap = new HashMap<>();
        try {
            for (JanusLiveStream stream : portService.getAllActiveStreams()) {
                activeStreamsMap.put(stream.getJanusRoomId(), stream);
            }
        } catch (Exception e) {
            // DB error fallback
        }

        for (Map<String, Object> room : rooms) {
            Number roomIdNum = (Number) room.get("room");
            if (roomIdNum != null) {
                Long roomId = roomIdNum.longValue();
                JanusLiveStream activeStream = activeStreamsMap.get(roomId);
                if (activeStream != null) {
                    room.put("is_streaming", true);
                    room.put("station_id", activeStream.getStationId());
                } else {
                    room.put("is_streaming", false);
                }
            }
        }

        return rooms;
    }

    /**
     * Отримання глобального api_secret медіасервера.
     */
    public String getApiSecret() {
        return janusClient.getApiSecret();
    }
}
