package media.toloka.rfa.config;

import media.toloka.rfa.media.messanger.service.ChatPresenceService;
import media.toloka.rfa.media.messanger.service.ChatReferenceSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ApplicationStartup
        implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private ChatPresenceService chatPresenceService;

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        /**
         * This event is executed as late as conceivably possible to indicate that
         * the application is ready to service requests.
         */
        ChatReferenceSingleton chatReference = ChatReferenceSingleton.getInstance();
        
        // Запитуємо актуальний стан чату від інших вузлів кластера
        chatPresenceService.requestGlobalSync();

    }

}