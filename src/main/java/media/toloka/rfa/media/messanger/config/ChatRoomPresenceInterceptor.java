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

/**
 * 🔌 Interceptor для відстеження STOMP підписок на кімнати чату.
 *
 * Перехоплює команди SUBSCRIBE/UNSUBSCRIBE на топіки формату /topic/{roomUuid}
 * та оновлює ChatRoomPresenceService.
 *
 * Фільтр: обробляє тільки топіки, які є UUID кімнат (36 символів з дефісами),
 * щоб не реагувати на службові топіки на кшталт /topic/chat.public.xxx.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomPresenceInterceptor implements ChannelInterceptor {

    private final ChatRoomPresenceService presenceService;

    /**
     * UUID кімнати має формат: 8-4-4-4-12 (36 символів)
     * Приклад: 0a47b08c-4b4d-4708-aa41-c0f04da510da
     */
    private static final String UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        StompCommand command = accessor.getCommand();
        String destination = accessor.getDestination();
        Principal user = accessor.getUser();

        if (user == null || destination == null) return message;

        String userUuid = user.getName();

        // Перевіряємо, чи це підписка на кімнату чату (формат: /topic/{UUID})
        if (destination.startsWith("/topic/")) {
            String potentialRoomUuid = destination.substring("/topic/".length());

            // Обробляємо тільки топіки, які є UUID кімнат (не службові топіки)
            if (potentialRoomUuid.matches(UUID_REGEX)) {
                if (command == StompCommand.SUBSCRIBE) {
                    log.info("📡 Користувач {} підписався на кімнату {}", userUuid, potentialRoomUuid);
                    presenceService.addUserToRoom(userUuid, potentialRoomUuid);
                } else if (command == StompCommand.UNSUBSCRIBE) {
                    log.info(" Користувач {} відписався від кімнати {}", userUuid, potentialRoomUuid);
                    presenceService.removeUserFromRoom(userUuid, potentialRoomUuid);
                }
            }
        }

        return message;
    }
}