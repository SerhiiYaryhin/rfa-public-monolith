package media.toloka.rfa.radio.model;
// Повідомлення в чаті.


import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;



import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(indexes = @Index(columnList = "uuid"))
public class Messages {
    @Id
    @GeneratedValue
    @Expose
    private Long id;
    @Expose
    private String uuid= UUID.randomUUID().toString();
    @Expose
    @ManyToOne(optional = false, cascade = {CascadeType.ALL}) //, cascade = {CascadeType.ALL}
//    @JoinColumn(name = "from_id")
    private Clientdetail from; // from user
    @Expose
    @ManyToOne(optional = false, cascade = {CascadeType.ALL}) //, cascade = {CascadeType.ALL}
//    @JoinColumn(name = "to_id")
    private Clientdetail to; // to user
    @Expose
    private boolean reading;
    @Expose
    private Date send;
    @Expose
    private Date read;
    @Expose
    @Column(columnDefinition = "TEXT")
    private String body;

    private String roomuuid = null;
//    @Expose
//    @OneToMany(cascade = {CascadeType.ALL})
//    private List<MessageRoom> room;

}