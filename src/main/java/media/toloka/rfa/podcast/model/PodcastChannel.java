package media.toloka.rfa.podcast.model;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
//import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;
import media.toloka.rfa.radio.store.model.Store;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(indexes = {@Index(columnList = "uuid"), @Index(columnList = "id"), @Index(columnList = "linktoimporturl")})
public class PodcastChannel {
    @Id
    @Expose
    private String uuid;

//    @GeneratedValue
    @Expose
    private Long id;

    @Expose
    private Boolean apruve = false;  // схвалення для публікації
    @Expose
    private String title; // Назва подкасту
    @Expose
    @Column(columnDefinition = "TEXT")
    private String description; // опис подкасту
    @Expose
    private String link; // Напевно, посилання на RSS подкасту на іншому ресурсі

    @Expose
    private String linktoimporturl=null; // linktoimporturl - посилання на RSS подкасту на ресурсі, з якого ми імпортували. Використовується для оновлення подкасту
    @Expose
    @DateTimeFormat(pattern = "dd-MM-yy")
    private Date lastbuilddate = new Date(); // дата останього оновлення.
    @Expose
    private String language="uk"; // мова подкасту
    @Expose
    private String copyright="CC BY 4.0"; // ліцензія
    @Expose
    @DateTimeFormat(pattern = "dd-MM-yy")
    private Date date = new Date(); // Дата створення запису
    @Expose
    @DateTimeFormat(pattern = "dd-MM-yy")
    private Date datepublish=null; // дата публікації
    @Expose
    private Boolean publishing = false;  // опубліковано автором
    @Expose
    private Boolean explicit = false; // відвертий вміст
    @Expose
    private Long looked = 0L; // скільки разів подивилися

    @ToString.Exclude
    @ElementCollection
    @OneToMany(orphanRemoval = true, fetch=FetchType.LAZY, cascade = {CascadeType.ALL})
    private List<PodcastItem> item = new ArrayList<>(); // перелік епізодів

    @ToString.Exclude
    @ElementCollection
    @OneToMany(orphanRemoval = true, mappedBy = "chanel", fetch=FetchType.LAZY)
    private List<PodcastItunesCategory> itunescategory = new ArrayList<>();  // категорія подкасту

    @Expose
//    @ToString.Exclude
    @OneToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "imagestoreuuid")
    private Store imagechanelstore;

//    @ToString.Exclude
//    @ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
//    @JoinColumn(name = "clientdetail_id", nullable = true )
//    private Clientdetail clientdetail;  // посилання на запис аутентифікації автора подкасту.
//    @Expose

    @ToString.Exclude
    private String clientdetail;

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
