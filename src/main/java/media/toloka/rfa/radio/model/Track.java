package media.toloka.rfa.radio.model;


import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import media.toloka.rfa.radio.model.enumerate.EDocumentStatus;
import media.toloka.rfa.radio.store.model.Store;

import java.util.Date;
import java.util.UUID;

@Data
@Entity
@Table(indexes = @Index(columnList = "uuid"))
public class Track {
    @Id
    @GeneratedValue
    @Expose
    private Long id;

    @Expose
    private String uuid= UUID.randomUUID().toString();

    @Expose
    private Date uploaddate = new Date();

    @Expose
    private String name;

    @Expose
    private String autor;

    @Expose
    @OneToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "store_id")
    private Store storeitem;

    @Expose
    private String storeuuid;

    @Expose
    @Column(columnDefinition = "TEXT")
    private String description;

    @Expose
    private String style;

    @Expose
    private Boolean notnormalvocabulary = false;

    @Expose
    private Boolean tochat = true;

    @Expose
    private EDocumentStatus status;

    @ToString.Exclude
    @ManyToOne(optional = true, fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
    @JoinColumn(name = "album_id")
    private Album album;

    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
    @JoinColumn(name = "clientdetail_id")
    private Clientdetail clientdetail;
}
