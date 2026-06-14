package media.toloka.rfa.media.messanger.model;

import lombok.Data;

@Data
public class ChatListElement {
    private String uuid;
    private String name;
    private int unreadCount;
}
