package media.toloka.rfa.radio.stt.model;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.store.model.Store;
import org.hibernate.annotations.UuidGenerator;

import java.util.Date;
import java.util.UUID;

import static media.toloka.rfa.radio.newstoradio.model.ENewsCategory.NEWS_CATEGORY_NEWS;
import static media.toloka.rfa.radio.newstoradio.model.ENewsStatus.NEWS_STATUS_CREATE;

@Data
@Entity
@Table(indexes = {@Index(columnList = "uuid"), @Index(columnList = "id")})
public class Stt {
    @Id
    @UuidGenerator
    @Expose
    private String uuid ; // = UUID.randomUUID().toString();
    @Expose
    private Long id;
    @Expose
    private String title ="";
    @Expose
    @Column(columnDefinition = "TEXT")
    private String runbody = "";
    @Expose
    @Column(columnDefinition = "TEXT")
    private String text = "";
    @Expose
    @Column(columnDefinition = "TEXT")
    private String jsonresult = " ";
    private Date createdate = new Date();
    private ESttStatus status = ESttStatus.STT_STATUS_CREATE;
    private Date datechangestatus = new Date();
    private ESttModel model = ESttModel.STT_MODEL_TURBO;

    @ToString.Exclude
    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.DETACH, CascadeType.REFRESH})
    private Store storespeach;

//    @ToString.Exclude
//    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.DETACH, CascadeType.REFRESH})
//    private Store storetext;
//
//    @ToString.Exclude
//    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.DETACH, CascadeType.REFRESH})
//    private Store storesubtitle;

    @ToString.Exclude
    @ManyToOne( fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.DETACH, CascadeType.REFRESH})
    @JoinColumn(name = "clientdetail_id")
    private Clientdetail clientdetail;

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
