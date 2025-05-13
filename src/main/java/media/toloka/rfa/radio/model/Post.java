package media.toloka.rfa.radio.model;


import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import media.toloka.rfa.radio.model.enumerate.EPostCategory;
//import media.toloka.rfa.radio.model.PostCategory;
import media.toloka.rfa.radio.model.enumerate.EPostStatus;
import media.toloka.rfa.radio.store.model.Store;
//import media.toloka.rfa.security.model.Users;

import java.util.Date;
import java.util.UUID;

@Data
@Entity
@Table(indexes = @Index(columnList = "uuid"))
public class Post {
    @Id
    @GeneratedValue
    @Expose
    private Long id;
    @Expose
    private String uuid= UUID.randomUUID().toString();
    @Expose
    private String posttitle; // заголовок посту
    @Expose
    @Column(columnDefinition = "TEXT")
    private String postbody;  // тіло посту
    @Expose
    private Date createdate = new Date();  // дата створення посту
    @Expose
    private Date publishdate;  // дата публікації посту
    @Expose
    private EPostStatus postStatus;  // статус посту
    @Expose
    private Date enddate;  // дата до якої пост відображається на сайті. Використовувати для анонсу подій.
    @Expose
    private EPostCategory category;  // категорія посту
    @Expose
    private String coverstoreuuid;  // посилання на головну ілюстрацію у сховищі.
    @Expose
    @ManyToOne(cascade = {CascadeType.ALL})
    private PostCategory postcategory = null;   // ще одна категорія? todo розібратися 25.05.13

    @ToString.Exclude
    @OneToOne(cascade = {CascadeType.ALL})
    private Store store;  // уяви не маю що це :(
//    @ManyToOne(optional = false, fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
//    @JoinColumn(name = "store_id")
//    private Store store;

    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
    @JoinColumn(name = "clientdetail_id")
    private Clientdetail clientdetail;

    @Expose
    private Boolean apruve = false;  // пост схвалено редактором для публікації
    @Expose
    private Date apruvedate; // дата схвалення публікації
    @Expose
    private Long looked = 0L; // скільки разів подивилися
//    @Expose
//    private Clientdetail apruveuser;

}
