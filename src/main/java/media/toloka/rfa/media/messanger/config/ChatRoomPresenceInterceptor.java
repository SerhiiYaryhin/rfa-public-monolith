package media.toloka.rfa.media.messanger.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.media.messanger.service.ChatRoomPresenceService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomPresenceInterceptor implements ChannelInterceptor {

    private final ChatRoomPresenceService presenceService;
    private final WebSocketEventListener webSocketEventListener;

    private static final String UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        StompCommand command = accessor.getCommand();
        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId();

        if (sessionId == null || destination == null) return message;

        // 1. Спочатку пробуємо отримати UUID з мапи сесій
        String userUuid = webSocketEventListener.getUserUuidBySession(sessionId);

        // 2. Fallback: якщо мапінгу немає, пробуємо отримати з Principal (email)
        if (userUuid == null) {
            Principal user = accessor.getUser();
            if (user != null) {
                userUuid = user.getName();
            }
        }

        if (userUuid == null) return message;

        // Перевіряємо, чи це підписка на кімнату чату (формат: /topic/{UUID})
        if (destination.startsWith("/topic/")) {
            String potentialRoomUuid = destination.substring("/topic/".length());

            if (potentialRoomUuid.matches(UUID_REGEX)) {
                if (command == StompCommand.SUBSCRIBE) {
                    log.info("📡 Користувач {} (Session: {}) підписався на кімнату {}", userUuid, sessionId, potentialRoomUuid);
                    presenceService.addUserToRoom(userUuid, potentialRoomUuid);
                } else if (command == StompCommand.UNSUBSCRIBE) {
                    log.info("🔌 Користувач {} (Session: {}) відписався від кімнати {}", userUuid, sessionId, potentialRoomUuid);
                    presenceService.removeUserFromRoom(userUuid, potentialRoomUuid);
                }
            }
        }
        return message;
    }
}