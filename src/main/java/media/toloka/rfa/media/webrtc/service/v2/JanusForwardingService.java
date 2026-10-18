package media.toloka.rfa.media.messanger.webrtc.service.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 📡 Сервіс ретрансляції (RTP Forward) аудіопотоків.
 * Відповідає за запуск та зупинку трансляції міксу кімнати назовні.
 */
@Service
public class JanusForwardingService {

    private static final Logger log = LoggerFactory.getLogger(JanusForwardingService.class);

    private final JanusAmqpClient amqpClient;
    private final JanusSessionManager sessionManager;

    @Value("${janus.secret}")
    private String apiSecret;

    public JanusForwardingService(JanusAmqpClient amqpClient, JanusSessionManager sessionManager) {
        this.amqpClient = amqpClient;
        this.sessionManager = sessionManager;
    }

    /**
     * 📡 Запуск ретрансляції (RTP Forward) аудіоміксу кімнати на вказану IP-адресу та UDP-порт.
     *
     * @param roomId ID кімнати
     * @param host IP отримувача
     * @param rtpPort Порт отримувача
     * @return stream_id для подальшої зупинки
     */
    @SuppressWarnings("unchecked")
    public Long createRtpForward(Long roomId, String host, Integer rtpPort) {
        if (roomId == null || rtpPort == null || host == null) {
            throw new IllegalArgumentException("roomId, host та rtpPort не можуть бути null");
        }

        log.info("Створення RTP forward в кімнаті {} на {}:{}", roomId, host, rtpPort);

        return sessionManager.executeInSession((sessionId, handleId) -> {
            String transactionId = amqpClient.generateTransactionId("rtp-forward");
            Map<String, Object> request = amqpClient.createBaseRequest("message", transactionId, apiSecret);
            request.put("session_id", sessionId);
            request.put("handle_id", handleId);

            Map<String, Object> body = new HashMap<>();
            body.put("request", "rtp_forward");
            body.put("room", roomId);
            body.put("secret", apiSecret);
            body.put("host", host);
            body.put("port", rtpPort);
            body.put("ptype", 100);
            body.put("ssrc", roomId * 1000); // Генеруємо унікальний SSRC для трансляції
            request.put("body", body);

            Map<String, Object> response = amqpClient.sendAndReceive(request);
            amqpClient.validatePluginResponse(response);

            Map<String, Object> plugindata = (Map<String, Object>) response.get("plugindata");
            Map<String, Object> data = (Map<String, Object>) plugindata.get("data");

            return ((Number) data.get("stream_id")).longValue();
        });
    }

    /**
     * ⏹️ Зупинка ретрансляції (RTP Forward).
     */
    public void stopRtpForward(Long roomId, Long streamId) {
        if (roomId == null || streamId == null) {
            throw new IllegalArgumentException("roomId та streamId не можуть бути null");
        }

        log.info("Зупинка RTP forward в кімнаті {} з stream_id={}", roomId, streamId);

        sessionManager.executeInSession((sessionId, handleId) -> {
            String transactionId = amqpClient.generateTransactionId("rtp-stop");
            Map<String, Object> request = amqpClient.createBaseRequest("message", transactionId, apiSecret);
            request.put("session_id", sessionId);
            request.put("handle_id", handleId);

            Map<String, Object> body = new HashMap<>();
            body.put("request", "stop_rtp_forward");
            body.put("room", roomId);
            body.put("stream_id", streamId);
            body.put("secret", apiSecret);
            request.put("body", body);

            Map<String, Object> response = amqpClient.sendAndReceive(request);
            amqpClient.validatePluginResponse(response);

            log.info("RTP forward успішно зупинено");
            return null;
        });
    }
}