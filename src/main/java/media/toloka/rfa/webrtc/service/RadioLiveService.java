package media.toloka.rfa.webrtc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import media.toloka.rfa.webrtc.model.JanusLiveStream;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class RadioLiveService {

    private final JanusRabbitClient janusClient;
    private final PortAllocationService portService;
    private final RabbitTemplate rabbitTemplate; // Використовуємо для відправки команд в RabbitMQ
    private final ObjectMapper objectMapper;     // Для генерації JSON

    public RadioLiveService(JanusRabbitClient janusClient,
                            PortAllocationService portService,
                            RabbitTemplate rabbitTemplate,
                            ObjectMapper objectMapper) {
        this.janusClient = janusClient;
        this.portService = portService;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public JanusLiveStream startBroadcast(String stationId, String sourceStreamUrl) {
        return portService.getStreamByStation(stationId).orElseGet(() -> {
            Integer freePort = portService.findNextFreePort();

            // 1. Створюємо кімнату в Janus
            Long janusRoomId = janusClient.createDynamicRoomWithRadio("Ефір: " + stationId, freePort);

            // 2. Формуємо та відправляємо команду на запуск через RabbitMQ
            sendFfmpegCommand("start", stationId, sourceStreamUrl, "rtp://janus.rfa:" + freePort);

            // Зберігаємо стан (ffmpegPid можна тимчасово поставити 0,
            // оскільки процес тепер керується Python-сервісом)
            return portService.saveActiveStream(stationId, janusRoomId, freePort, 0);
        });
    }

    @Transactional
    public void stopBroadcast(String stationId) {
        portService.getStreamByStation(stationId).ifPresent(stream -> {
            // 1. Надсилаємо команду на зупинку через RabbitMQ
            sendFfmpegCommand("stop", stationId, null, null);

            // 2. Видаляємо дані про потік
            portService.deleteStream(stationId);
        });
    }

    /**
     * Допоміжний метод для відправки JSON у RabbitMQ
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

            String jsonPayload = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend("ffmpeg-agent-commands", jsonPayload);

        } catch (Exception e) {
            throw new RuntimeException("Не вдалося відправити команду FFmpeg через RabbitMQ", e);
        }
    }
}