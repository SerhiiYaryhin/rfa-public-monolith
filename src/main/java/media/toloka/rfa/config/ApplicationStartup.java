package media.toloka.rfa.config;

import media.toloka.rfa.media.messenger.service.ChatReferenceSingleton;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ApplicationStartup
        implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        /**
         * This event is executed as late as conceivably possible to indicate that
         * the application is ready to service requests.
         */
        ChatReferenceSingleton chatReference = ChatReferenceSingleton.getInstance();
        Map<String, String> roomList =  chatReference.GetRoomsMap();
        if (roomList.size() == 0) {


        }

    }

}