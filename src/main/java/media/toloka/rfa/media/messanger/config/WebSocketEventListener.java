package media.toloka.rfa.media.messanger.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.media.messanger.model.ChatPresenceEvent;
import media.toloka.rfa.media.messanger.service.ChatPresenceService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Слухач подій WebSocket для автоматичного відстеження підключень та відключень.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final ChatPresenceService presenceService;
    private final ClientService clientService;
    
    // Тимчасове мапування сесії на UUID користувача для коректного відключення
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        log.debug("Нове WebSocket з'єднання...");
    }

    /**
     * Ми використовуємо подію з контролера для реєстрації, але тут теж можна перехоплювати.
     * Проте STOMP Connect не завжди містить UUID користувача у зручному вигляді.
     * Тому основна реєстрація йде через /app/hello.
     */

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String userUuid = sessionUserMap.remove(sessionId);

        if (userUuid != null) {
            log.info("Користувач {} відключився (Session: {})", userUuid, sessionId);
            presenceService.publishPresenceEvent(
                    ChatPresenceEvent.EventType.USER_DISCONNECTED,
                    userUuid,
                    null
            );
        }
    }

     /**
     * Допоміжний метод для зв'язування сесії з користувачем (викликається з контролера).
     */
    public void registerSession(String sessionId, String userUuid) {
        sessionUserMap.put(sessionId, userUuid);
    }

    /**
     * ✅ ДОДАНО: Отримати UUID користувача за sessionId (потрібно для ChatRoomPresenceInterceptor)
     */
    public String getUserUuidBySession(String sessionId) {
        return sessionUserMap.get(sessionId);
    }
}
