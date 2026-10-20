package media.toloka.rfa.media.webrtc.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
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
     * 💓 Heartbeat для FFmpeg агента.
     * Відправляється кожні 2 хвилини (120000 мс).
     * Якщо агент не отримає heartbeat протягом 5 хвилин, він автоматично зупинить FFmpeg.
     */
    @Scheduled(fixedRate = 120000)
    public void sendHeartbeat() {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("command", "heartbeat");
            message.put("timestamp", System.currentTimeMillis());

            String jsonPayload = objectMapper.writeValueAsString(message);
            log.debug("💓 Відправка Heartbeat FFmpeg агенту");

            rabbitTemplate.convertAndSend("ffmpeg-agent-commands", jsonPayload);
        } catch (Exception e) {
            log.error("❌ Помилка відправки Heartbeat: {}", e.getMessage(), e);
        }
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