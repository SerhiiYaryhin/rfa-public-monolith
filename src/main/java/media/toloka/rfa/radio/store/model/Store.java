package media.toloka.rfa.radio.store.model;
// https://paulcwarren.github.io/spring-content/refs/release/1.2.4/fs-index.html


import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import media.toloka.rfa.radio.model.Clientdetail;

import java.util.Date;
import java.util.UUID;

@Data
@Entity
@Table(indexes = {
        @Index(columnList = "uuid"),
        @Index(columnList = "id")
})
public class Store {
    @Id
    @GeneratedValue
    @Expose
    private Long id;
    @Expose
    private String uuid;


    @Expose
    private Long previd = 0L;
    @Expose
    private Long countversion = 0L;
    @Expose
    private EStoreFileType storefiletype;
    @Expose
    private String filename;
    @Expose
    private String filepatch;
    @Expose
    private Long filelength;
    @Expose
    private String contentMimeType;
    @Expose
    private Date createdate = new Date();
    @Expose
    private String comment;
    @Expose
    private Date preparedate = null;
    @Expose
    private Integer prepared;


    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER, cascade = {CascadeType.MERGE})
    @JoinColumn(name = "clientdetail_id")
    private Clientdetail clientdetail;

    @PrePersist
    public void generateUUID() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
    }

}
