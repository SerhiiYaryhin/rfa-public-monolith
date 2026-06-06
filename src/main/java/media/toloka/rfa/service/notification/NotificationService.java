package media.toloka.rfa.service.notification;

import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Messages;
import media.toloka.rfa.radio.repository.RepoMessages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class NotificationService {

    @Autowired
    private RepoMessages repoMessages;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    public void sendSystemNotification(Clientdetail from, Clientdetail to, String body) {
        Messages msg = new Messages();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setBody(body);
        msg.setSend(new Date());
        msg.setReading(false);
        repoMessages.save(msg);

        // Повідомлення через WebSocket, якщо налаштовано
        if (messagingTemplate != null) {
            try {
                messagingTemplate.convertAndSendToUser(
                    to.getUser().getEmail(), 
                    "/queue/notifications", 
                    body
                );
            } catch (Exception e) {
                // Логування помилки WebSocket не повинно переривати основний процес
            }
        }
    }
}
