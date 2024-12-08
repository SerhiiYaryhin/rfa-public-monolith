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
    private String posttitle;
    @Expose
    @Column(columnDefinition = "TEXT")
    private String postbody;
    @Expose
    private Date createdate = new Date();
    @Expose
    private Date publishdate;
    @Expose
    private EPostStatus postStatus;
    @Expose
    private Date enddate;
    @Expose
    private EPostCategory category;
    @Expose
    private String coverstoreuuid;
    @Expose
    @ManyToOne(cascade = {CascadeType.ALL})
    private PostCategory postcategory;

    @ToString.Exclude
    @OneToOne(cascade = {CascadeType.ALL})
    private Store store;
//    @ManyToOne(optional = false, fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
//    @JoinColumn(name = "store_id")
//    private Store store;

    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
    @JoinColumn(name = "clientdetail_id")
    private Clientdetail clientdetail;

    @Expose
    private Boolean apruve = false;
    @Expose
    private Date apruvedate;
    @Expose
    private Long looked = 0L; // скільки разів подивилися
//    @Expose
//    private Clientdetail apruveuser;

}
