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
public class Documents {

    @Id
    @GeneratedValue
    @Expose
    private Long id;
    @Expose
    private String uuid= UUID.randomUUID().toString();
    @Expose
    private Date loaddate;
//    @Expose
//    private String pathToDocument;
    @Expose
    private EDocumentStatus status;
    @Expose
    private String documenttype;

    @Expose
    private String storeuuid;

    @Expose
    @Column(columnDefinition="TEXT")
    private String userComment;
    @Expose
    @Column(columnDefinition="TEXT")
    private String adminComment;
//    @ElementCollection
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "clientdetail_id")
    private Clientdetail clientdetail;

    @OneToOne //(mappedBy = "store", cascade = {CascadeType.ALL})
    @JoinColumn(name = "store_id")
//    @Expose
    private Store store;

    public Documents() {
        this.loaddate = new Date();
        this.status = EDocumentStatus.STATUS_LOADED;
    }
}
