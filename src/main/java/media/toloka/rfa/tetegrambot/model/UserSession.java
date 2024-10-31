package media.toloka.rfa.tetegrambot.model;

import lombok.Builder;
import lombok.Data;
import media.toloka.rfa.tetegrambot.enums.ConversationState;
//import org.vladyka.enums.ConversationState;

@Data
@Builder
public class UserSession {
    private Long chatId;
    private ConversationState state;
    private String city;
    private String text;
}
