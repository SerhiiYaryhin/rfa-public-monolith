package media.toloka.rfa.podcast.model;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.store.model.Store;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.UUID;

@Data
@Entity
@Table(indexes = @Index(columnList = "uuid"))
public class PodcastImage {
    @Id
    @GeneratedValue
    @Expose
    private Long id;
    @Expose
    private String uuid = UUID.randomUUID().toString();
    @Expose
    private String title = "";
    @Expose
    private String url = "";
    @Expose
    private String originalurl = "";
    @Expose
    private String link ="";
    @Expose
    @DateTimeFormat(pattern = "dd-MM-yy")
    private Date date = new Date();

    @ManyToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "store_id")
    private Store storeidimage;

    @ToString.Exclude
//    @ManyToOne(optional = false, fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
    @ManyToOne(optional = false, fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
    @JoinColumn(name = "clientdetail_id")
    private Clientdetail clientdetail;
}
