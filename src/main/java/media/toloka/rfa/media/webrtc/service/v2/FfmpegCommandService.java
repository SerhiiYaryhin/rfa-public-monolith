package media.toloka.rfa.media.webrtc.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 🎬 Сервіс відправки команд FFmpeg агенту через RabbitMQ.
 */
@Service
public class FfmpegCommandService {

    private static final Logger log = LoggerFactory.getLogger(FfmpegCommandService.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public FfmpegCommandService(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Відправка команди старту FFmpeg.
     */
    public void sendStartCommand(String stationId, String streamUrl, String rtpUrl) {
        sendFfmpegCommand("start", stationId, streamUrl, rtpUrl);
    }

    /**
     * Відправка команди зупинки FFmpeg.
     */
    public void sendStopCommand(String stationId) {
        sendFfmpegCommand("stop", stationId, null, null);
    }

    /**
     * Відправка команди FFmpeg агенту через чергу RabbitMQ.
     */
    private void sendFfmpegCommand(String command, String stationId, String streamUrl, String rtpUrl) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("command", command);
            message.put("stationId", stationId);

            if ("start".equals(command)) {
                message.put("streamUrl", streamUrl);
                message.put("rtpUrl", rtpUrl);
                message.put("ffmpegOptions", Map.of(
                        "acodec", "libopus",
                        "b:a", "128k",
                        "ar", "48000",
                        "ac", "2",
                        "application", "audio",
                        "vbr", "on",
                        "compression_level", "10"
                ));
            }

            String jsonPayload = objectMapper.writeValueAsString(message);
            log.info("📤 Відправка команди FFmpeg: {}", jsonPayload);

            rabbitTemplate.convertAndSend("ffmpeg-agent-commands", jsonPayload);
            log.info("✅ Команду FFmpeg відправлено в RabbitMQ");
        } catch (Exception e) {
            log.error("❌ Помилка відправки команди FFmpeg: {}", e.getMessage(), e);
            throw new RuntimeException("Помилка відправки команди FFmpeg: " + e.getMessage());
        }
    }
}