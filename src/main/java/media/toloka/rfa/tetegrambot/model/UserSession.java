package media.toloka.rfa.tetegrambot.model;

import lombok.Builder;
import lombok.Data;
import media.toloka.rfa.media.messanger.model.MessageRoom;
import media.toloka.rfa.tetegrambot.enums.ConversationState;

@Data
@Builder
public class UserSession {
    private Long userId;
    private Long chatId;
    private String userFName;
    private String userSName;
    private String UserFirmName;
    private String UserPassword;
    private String userEmail;
    private ConversationState state;
    private String city;
    private String text;
    private MessageRoom RFA_Chat_Choose;
}
