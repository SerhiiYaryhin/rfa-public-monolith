package media.toloka.rfa.media.messanger.webrtc.service.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 🏠 Сервіс управління аудіокімнатами Janus AudioBridge.
 * Відповідає за створення, видалення та отримання інформації про кімнати.
 */
@Service
public class JanusRoomService {

    private static final Logger log = LoggerFactory.getLogger(JanusRoomService.class);

    private final JanusAmqpClient amqpClient;
    private final JanusSessionManager sessionManager;

    @Value("${janus.secret}")
    private String apiSecret;

    public JanusRoomService(JanusAmqpClient amqpClient, JanusSessionManager sessionManager) {
        this.amqpClient = amqpClient;
        this.sessionManager = sessionManager;
    }

    /**
     * 🎙️ Створення нової голосової кімнати в Janus AudioBridge.
     * Параметр allow_rtp_participants встановлюється в true для прийому Plain RTP потоку від FFmpeg.
     */
    public void createAudioBridgeRoom(Long roomId, String name) {
        if (roomId == null) {
            log.warn("Спроба створити кімнату в Janus із порожнім ID (null)");
            return;
        }

        log.info("Створення аудіокімнати в Janus. ID: {}, Назва: {}", roomId, name);

        try {
            sessionManager.executeInSession((sessionId, handleId) -> {
                String transactionId = amqpClient.generateTransactionId("room-create");
                Map<String, Object> request = amqpClient.createBaseRequest("message", transactionId, apiSecret);
                request.put("session_id", sessionId);
                request.put("handle_id", handleId);

                Map<String, Object> body = new HashMap<>();
                body.put("request", "create");
                body.put("room", roomId);
                body.put("description", name);
                body.put("secret", apiSecret);
                body.put("permanent", false);
                body.put("sampling_rate", 48000);
                body.put("audio_codec", "opus");
                body.put("is_private", false);
                body.put("audio_level_average", 25);
                body.put("allow_rtp_participants", true);
                request.put("body", body);

                Map<String, Object> response = amqpClient.sendAndReceive(request);
                amqpClient.validatePluginResponse(response);

                log.info("Аудіокімнату ID: {} ('{}') успішно створено з allow_rtp_participants=true", roomId, name);
                return null;
            });
        } catch (Exception e) {
            log.error("Не вдалося створити кімнату {} в Janus: {}", roomId, e.getMessage(), e);
            throw new RuntimeException("Помилка створення кімнати в Janus", e);
        }
    }

    /**
     * 💥 Видалення аудіокімнати з Janus.
     */
    public void destroyAudioBridgeRoom(Long janusRoomId) {
        if (janusRoomId == null) {
            log.warn("Спроба видалити кімнату з Janus із порожнім ID (null)");
            return;
        }

        log.info("Видалення аудіокімнати ID: {} з Janus", janusRoomId);

        try {
            sessionManager.executeInSession((sessionId, handleId) -> {
                String transactionId = amqpClient.generateTransactionId("room-destroy");
                Map<String, Object> request = amqpClient.createBaseRequest("message", transactionId, apiSecret);
                request.put("session_id", sessionId);
                request.put("handle_id", handleId);

                Map<String, Object> body = new HashMap<>();
                body.put("request", "destroy");
                body.put("room", janusRoomId);
                body.put("secret", apiSecret);
                request.put("body", body);

                Map<String, Object> response = amqpClient.sendAndReceive(request);
                amqpClient.validatePluginResponse(response);

                log.info("Аудіокімнату ID: {} успішно видалено з Janus", janusRoomId);
                return null;
            });
        } catch (Exception e) {
            log.error("Не вдалося видалити кімнату {} з Janus: {}", janusRoomId, e.getMessage(), e);
        }
    }

    /**
     * 🔍 Отримання списку всіх кімнат та їхніх поточних учасників.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getJanusRoomsAndParticipants() {
        return sessionManager.executeInSession((sessionId, handleId) -> {
            String transactionId = amqpClient.generateTransactionId("rooms-list");
            Map<String, Object> request = amqpClient.createBaseRequest("message", transactionId, apiSecret);
            request.put("session_id", sessionId);
            request.put("handle_id", handleId);
            request.put("body", Map.of("request", "list"));

            Map<String, Object> response = amqpClient.sendAndReceive(request);
            amqpClient.validatePluginResponse(response);

            Map<String, Object> plugindata = (Map<String, Object>) response.get("plugindata");
            if (plugindata == null) return List.of();

            Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
            if (data == null) return List.of();

            List<Map<String, Object>> rooms = (List<Map<String, Object>>) data.get("list");
            if (rooms == null) return List.of();

            // Для кожної знайденої кімнати дозавантажуємо список учасників
            for (Map<String, Object> room : rooms) {
                try {
                    Number roomIdNum = (Number) room.get("room");
                    if (roomIdNum == null) continue;

                    Long roomId = roomIdNum.longValue();
                    String partTransactionId = amqpClient.generateTransactionId("part-list");

                    Map<String, Object> partRequest = amqpClient.createBaseRequest("message", partTransactionId, apiSecret);
                    partRequest.put("session_id", sessionId);
                    partRequest.put("handle_id", handleId);
                    partRequest.put("body", Map.of("request", "listparticipants", "room", roomId));

                    Map<String, Object> partResponse = amqpClient.sendAndReceive(partRequest);
                    amqpClient.validatePluginResponse(partResponse);

                    Map<String, Object> partPluginData = (Map<String, Object>) partResponse.get("plugindata");
                    Map<String, Object> partData = (Map<String, Object>) partPluginData.get("data");
                    List<Map<String, Object>> participants = (List<Map<String, Object>>) partData.get("participants");

                    room.put("participants", participants != null ? participants : List.of());
                } catch (Exception e) {
                    log.error("Помилка отримання учасників для кімнати {}", room.get("room"), e);
                    room.put("participants", List.of());
                }
            }

            return rooms;
        });
    }

    /**
     * 🔍 Отримання списку числових ідентифікаторів усіх кімнат у Janus.
     */
    @SuppressWarnings("unchecked")
    public List<Long> getActiveRoomsIds() {
        try {
            return sessionManager.executeInSession((sessionId, handleId) -> {
                String transactionId = amqpClient.generateTransactionId("rooms-ids");
                Map<String, Object> request = amqpClient.createBaseRequest("message", transactionId, apiSecret);
                request.put("session_id", sessionId);
                request.put("handle_id", handleId);
                request.put("body", Map.of("request", "list"));

                Map<String, Object> response = amqpClient.sendAndReceive(request);
                amqpClient.validatePluginResponse(response);

                Map<String, Object> plugindata = (Map<String, Object>) response.get("plugindata");
                Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
                List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("list");

                if (list == null) return List.of();

                return list.stream()
                        .map(m -> ((Number) m.get("room")).longValue())
                        .collect(Collectors.toList());
            });
        } catch (Exception e) {
            log.error("Помилка отримання ID кімнат Janus: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 👥 Перевіряє, чи присутній користувач із заданим токеном (UUID) у кімнаті.
     */
    @SuppressWarnings("unchecked")
    public boolean isUserInAudioRoom(Long roomId, String userToken) {
        return sessionManager.executeInSession((sessionId, handleId) -> {
            String transactionId = amqpClient.generateTransactionId("check-user");
            Map<String, Object> request = amqpClient.createBaseRequest("message", transactionId, apiSecret);
            request.put("session_id", sessionId);
            request.put("handle_id", handleId);
            request.put("body", Map.of("request", "listparticipants", "room", roomId));

            try {
                Map<String, Object> response = amqpClient.sendAndReceive(request);
                amqpClient.validatePluginResponse(response);

                Map<String, Object> plugindata = (Map<String, Object>) response.get("plugindata");
                if (plugindata == null) return false;

                Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
                if (data == null || data.get("participants") == null) return false;

                List<Map<String, Object>> participants = (List<Map<String, Object>>) data.get("participants");
                for (Map<String, Object> p : participants) {
                    String participantToken = (String) p.get("token");
                    if (userToken.equals(participantToken)) {
                        return true;
                    }
                }
            } catch (Exception e) {
                log.warn("Не вдалося отримати список учасників для кімнати {}", roomId, e);
            }

            return false;
        });
    }
}