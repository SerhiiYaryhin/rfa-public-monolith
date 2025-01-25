package media.toloka.rfa.podcast.model;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import media.toloka.rfa.radio.model.Clientdetail;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(indexes = @Index(columnList = "uuid"))
public class PodcastChannel {
    @Id
    @GeneratedValue
    @Expose
    private Long id;
    @Expose
    private String uuid = UUID.randomUUID().toString();
    @Expose
    private Boolean apruve = false;  // схвалення для публікації
    @Expose
    private String title; // Назва подкасту
    @Expose
    @Column(columnDefinition = "TEXT")
    private String description; // опис подкасту
    @Expose
    private String link; // Напевно, посилання на RSS подкасту на іншому ресурсі
    // linktoimporturl - посилання на RSS подкасту на ресурсі, з якого ми імпортували
    // використовується для оновлення епізодів
    @Expose
    private String linktoimporturl;
    @Expose
    @DateTimeFormat(pattern = "dd-MM-yy")
    private Date lastbuilddate = new Date(); // дата останього оновлення.
    @Expose
    private String language; // мова подкасту
    @Expose
    private String copyright; // ліцензія
    @Expose
    @DateTimeFormat(pattern = "dd-MM-yy")
    private Date date = new Date(); // Дата створення запису
    @Expose
    @DateTimeFormat(pattern = "dd-MM-yy")
    private Date datepublish; // дата публікації
    @Expose
    private Boolean publishing = false;  // опубліковано автором
    @Expose
    private Boolean explicit = false; // відвертий вміст
    @Expose
    private Long looked = 0L; // скільки разів подивилися

    @ManyToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "podcast_image_id")
    private PodcastImage image; // картинка подкасту

    @ElementCollection
    @OneToMany( fetch=FetchType.EAGER, cascade = {CascadeType.ALL})
    private List<PodcastItem> item = new ArrayList<>(); // перелік епізодів

//    @ElementCollection
    @OneToMany(mappedBy = "chanel", fetch=FetchType.EAGER, orphanRemoval = true)
//    @JoinColumn(name = "podcast_itunes_category_id")
    private List<PodcastItunesCategory> itunescategory = new ArrayList<>();  // категорія подкасту

//    @Expose
//    @OneToOne(cascade = {CascadeType.ALL})
//    @JoinColumn(name = "store_id")
//    private Store imagestoreitem;

//    @Expose
//    private String imagestoreuuid;

    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
    @JoinColumn(name = "clientdetail_id")
    private Clientdetail clientdetail;  // посилання на запис аутентифікації автора подкасту.
}
