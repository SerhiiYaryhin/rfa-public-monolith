package media.toloka.rfa.media.messanger.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO для передачі подій присутності (presence) через RabbitMQ.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatPresenceEvent implements Serializable {

    public enum EventType {
        USER_CONNECTED,
        USER_DISCONNECTED,
        ROOM_UPDATED,
        ROOM_DELETED,
        SYNC_REQUEST
    }

    private EventType type;
    private String uuid;
    private String name;
    private long timestamp;
}
