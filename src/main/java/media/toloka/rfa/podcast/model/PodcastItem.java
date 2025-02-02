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
@Table(indexes = {
        @Index(columnList = "uuid"),
        @Index(columnList = "id"),
        @Index(columnList = "storeuuid")
})
public class PodcastItem {
    @Id
    @Expose
    private String uuid;

    @Expose
    @GeneratedValue
    private Long id;

    @Expose
    private String title;
    @Expose
    private String link;
    @Expose
    private String pubDate;
    @Expose
    private String comments;
//    @Expose
//    private String category;
    @Expose
    private String timetrack;

    @Expose
    @Column(columnDefinition = "TEXT")
    private String description;
    @Expose
    private String enclosure;
    @Expose
    private String originalenclosure;
    @Expose
    @DateTimeFormat(pattern = "dd-MM-yy")
    private Date date = new Date();
    @Expose
    private String storeuuid;
    @Expose
    private Boolean explicit = false; // відвертий вміст
    @Expose
    private Long looked = 0L; // скільки разів подивилися

//    @OneToOne(cascade = {CascadeType.ALL})
    // аудіофайл подкасту
    @OneToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "store_id")
    private Store storeenclosure;


    @ElementCollection
//    @ManyToOne(cascade = CascadeType.ALL)
    @ToString.Exclude
    @ManyToOne(fetch=FetchType.EAGER, cascade = {CascadeType.ALL})
    @JoinColumn(name = "podcast_channel_uuid")
    private PodcastChannel chanel;

    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "clientdetail_id")
    private Clientdetail clientdetail;

    @ManyToOne (cascade = {CascadeType.ALL})
    @JoinColumn(name = "store_uuid")
    private Store imagestoreitem;

    // тимчасове поле. PodcastImage буде видалено
//    @ElementCollection
//    @ManyToOne(fetch=FetchType.EAGER, cascade = {CascadeType.ALL})
//    @JoinColumn(name = "podcast_image_id")
//    private PodcastImage image;


    @PrePersist
    public void generateUUID() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
    }
}
