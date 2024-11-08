package media.toloka.rfa.media.messanger.model;


import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import media.toloka.rfa.media.messanger.model.enumerate.EChatRoomType;

import java.util.Date;
import java.util.UUID;

@Data
@Entity
@Table(indexes = @Index(columnList = "uuid"))
public class MessageRoom {
    @Id
    @GeneratedValue
    @Expose
    private Long id;
    @Expose
    private String uuid= UUID.randomUUID().toString();
    @Expose
    private String roomname;
    @Expose
    private EChatRoomType roomtype; // тип кімнати - публічна або радіостанції
    @Expose
    private Boolean roomOnlineStatus = false; // кімната в онлайні?
    @Expose
    private Date startonline = null; // о котрій був перехід в онлайн
}
