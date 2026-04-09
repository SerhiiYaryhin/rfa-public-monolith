package media.toloka.rfa.media.messenger.model;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

@Entity
@Data
@Table(indexes = @Index(columnList = "uuid"))
public class ChatMessage {

    @Id
    @GeneratedValue
    @Expose
    private Long id;
    @Expose
    private String uuid= UUID.randomUUID().toString();
//    @Expose
//    @ManyToOne(optional = false, cascade = {CascadeType.ALL}) //, cascade = {CascadeType.ALL}
////    @JoinColumn(name = "from_id")
//    private Clientdetail from; // from user
//    @Expose
//    @ManyToOne(optional = false, cascade = {CascadeType.ALL}) //, cascade = {CascadeType.ALL}
////    @JoinColumn(name = "to_id")
//    private Clientdetail to; // to user
    @Expose
    private Date send = new Date();
    @Expose
    private String fromname;
    @Expose
    private String fromuuid;
    @Expose
    private String toname;
    @Expose
    private String touuid;
    @Expose
    @Column(columnDefinition = "TEXT")
    private String body;
    @Expose
    private Boolean reading = false;
    @Expose
    private Date read = null;
    @Expose
    private String roomuuid;
    @Expose
    private Integer rtype;
//    private EChatRecordType rtype;

}
