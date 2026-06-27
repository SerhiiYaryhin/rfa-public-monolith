package media.toloka.rfa.media.webrtc.temporary;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class JanusAdminService {

    private final String janusAdminWsUrl = "ws://janus.rfa:8189";
    private final String adminSecret = "RFA_Super_Secret_Admin_Key";
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Створює нову аудіокімнату в Janus AudioBridge для радіостанції
     * @param roomId унікальний числовий ідентифікатор кімнати
     * @param description назва радіостанції
     */
    public CompletableFuture<Boolean> createAudioRoom(long roomId, String description) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        StandardWebSocketClient client = new StandardWebSocketClient();

        try {
            client.execute(new TextWebSocketHandler() {
                @Override
                public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                    // Формуємо JSON-запит до Janus Admin API згідно з документацією AudioBridge
                    Map<String, Object> request = new HashMap<>();
                    request.put("janus", "admin_message");
                    request.put("transaction", UUID.randomUUID().toString());
                    request.put("admin_secret", adminSecret);

                    Map<String, Object> message = new HashMap<>();
                    message.put("plugin", "janus.plugin.audiobridge");

                    Map<String, Object> data = new HashMap<>();
                    data.put("request", "create");
                    data.put("room", roomId);
                    data.put("description", description);
                    data.put("sampling", 16000); // частота дискретизації для Opus кодека
                    data.put("record", false);   // чи записувати ефір у файл

                    message.put("request", data);
                    request.put("message", message);

                    String jsonPayload = objectMapper.writeValueAsString(request);
                    session.sendMessage(new TextMessage(jsonPayload));
                }

                @Override
                protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                    // Тут ловимо відповідь від Janus
                    String payload = message.getPayload();
                    // Перевіряємо чи Janus повернув "success"
                    if (payload.contains("success")) {
                        future.complete(true);
                    } else {
                        future.complete(false);
                    }
                    session.close();
                }
            }, janusAdminWsUrl);

        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }
}