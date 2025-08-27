package media.toloka.rfa.banner.model;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import media.toloka.rfa.banner.model.enumerate.EBannerStyle;
import media.toloka.rfa.banner.model.enumerate.EBannerType;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.store.model.Store;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.UUID;

@Data
@Entity
@Table(indexes = {@Index(columnList = "uuid"), @Index(columnList = "id")})
public class Banner {
    @Id
    @Expose
    private String uuid = UUID.randomUUID().toString();

    @Expose
    private Long id;
    @Expose
    private Boolean approve = false;  // схвалення для публікації
    @Expose
    @DateTimeFormat(pattern = "dd-MM-yy")
    private Date aprovedate = null; // дата схвалення для публікації
    @Expose
    @DateTimeFormat(pattern = "dd-MM-yy")
    private Date startdate = null; // дата старту показу
    @Expose
    @DateTimeFormat(pattern = "dd-MM-yy")
    private Date enddate = null; // дата закінчення показу.
    @Expose
    private String title= null; // Заголовок текстового банеру
    @Expose
    private String link= null; // посилання на клієнта
    @Expose
//    @Column(columnDefinition = "TEXT")
    private String description= null; // текст банеру
    @Expose
    private Long views = 0L; // кількість демонстрацій
    @Expose
    private Long transition = 0L; // кількість переходів
    @Expose
    private Long priority = 0L; // пріоритет банера
    @Expose
    @DateTimeFormat(pattern = "dd-MM-yy")
    private Date lastview = null; // останній раз показувався
    @Expose
    private EBannerType bannertype = null; // тип банера
    // Стиль банеру
    // https://bannerboo.com/ua/blog/yaki-standartni-rozmiry-reklamnykh-baneriv
    // https://spideraf.com/articles/the-digital-advertisers-handbook-a-comprehensive-guide-to-ad-sizes-2023
    @Expose
    private EBannerStyle bannerstyle = null;
    @Expose
    private String uuidmedia = null; // uuid медіа у сховищі
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    private Store store = null; // посилання на медіа у сховищі

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clientdetail_id", nullable = true)
    private Clientdetail clientdetail;  // посилання на власника банеру.

    @PrePersist
    public void generateUUID() {
        if (this.id == null) {
            this.id = System.currentTimeMillis(); // Метод для генерації унікального ID
        }
    }
}

