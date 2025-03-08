package media.toloka.rfa.radio.newstoradio.model;


import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.radio.store.model.Store;
import org.hibernate.annotations.UuidGenerator;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static media.toloka.rfa.radio.newstoradio.model.ENewsCategory.NEWS_CATEGORY_NEWS;
import static media.toloka.rfa.radio.newstoradio.model.ENewsStatus.NEWS_STATUS_CREATE;

@Data
@Entity
@Table(indexes = {@Index(columnList = "uuid"), @Index(columnList = "id")})
public class News {
    @Id
    @UuidGenerator
    @Expose
    private String uuid ; // = UUID.randomUUID().toString();
//    @Expose
    private Long id;
//    @Expose
    private String newstitle ="";
//    @Expose
    @Column(columnDefinition = "TEXT")
    private String newsbody = "";
//    @Expose
    private Date createdate = new Date();
//    @Expose
    private ENewsCategory category = NEWS_CATEGORY_NEWS;

    private ENewsStatus status = NEWS_STATUS_CREATE;
    private Date datechangestatus = new Date();

    @ToString.Exclude
    @ManyToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "station_id")
    private Station station;

    @ToString.Exclude
    @OneToOne(cascade = {CascadeType.ALL})
    private Store storespeach;
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
    @JoinColumn(name = "clientdetail_id")
    private Clientdetail clientdetail;

//    @Expose
    private Long looked = 0L; // скільки разів подивилися

    @PrePersist
    public void generateUUID() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        if (this.id == null) {
            this.id = System.currentTimeMillis(); // Метод для генерації унікального ID
        }
    }

}
