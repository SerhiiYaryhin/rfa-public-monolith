package media.toloka.rfa.media.messanger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.media.messanger.config.ChatPresenceConfig;
import media.toloka.rfa.media.messanger.model.ChatPresenceEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Сервіс для синхронізації стану чату (користувачі та кімнати) через RabbitMQ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatPresenceService {

    private final RabbitTemplate rabbitTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Публікація події у RabbitMQ для розсилки всім вузлам.
     */
    public void publishPresenceEvent(ChatPresenceEvent.EventType type, String uuid, String name) {
        ChatPresenceEvent event = new ChatPresenceEvent(type, uuid, name, System.currentTimeMillis());
        log.debug("Публікація події присутності: {}", event);
        rabbitTemplate.convertAndSend(ChatPresenceConfig.CHAT_PRESENCE_EXCHANGE, "", event);
    }

    /**
     * Слухач подій від RabbitMQ. Виконується на кожному вузлі сервера.
     */
    @RabbitListener(queues = "#{chatPresenceQueue.name}")
    public void receivePresenceEvent(ChatPresenceEvent event) {
        log.debug("Отримано подію присутності від RabbitMQ: {}", event);
        
        ChatReferenceSingleton chatRef = ChatReferenceSingleton.getInstance();
        
        switch (event.getType()) {
            case USER_CONNECTED:
                chatRef.GetUsersMap().put(event.getUuid(), event.getName());
                chatRef.GetUserLastLiveTime().put(event.getUuid(), new Date(event.getTimestamp()));
                notifyClientsPresenceUpdate();
                break;
                
            case USER_DISCONNECTED:
                chatRef.GetUsersMap().remove(event.getUuid());
                chatRef.GetUserLastLiveTime().remove(event.getUuid());
                notifyClientsPresenceUpdate();
                break;
                
            case ROOM_UPDATED:
                chatRef.GetRoomsMap().put(event.getUuid(), event.getName());
                notifyClientsPresenceUpdate();
                break;
                
            case ROOM_DELETED:
                chatRef.GetRoomsMap().remove(event.getUuid());
                notifyClientsPresenceUpdate();
                break;
        }
    }

    /**
     * Повідомляємо всіх WebSocket клієнтів про зміну стану (через глобальний топік або індивідуально).
     * В даному випадку ми просто тригеримо оновлення списків на фронтенді.
     */
    private void notifyClientsPresenceUpdate() {
        // Відправляємо пусте повідомлення в спеціальний топік, щоб клієнти знали, що треба оновити списки
        // Або відправляємо сам факт зміни.
        messagingTemplate.convertAndSend("/topic/presence-update", "REFRESH");
    }
}
