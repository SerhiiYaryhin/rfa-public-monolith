package media.toloka.rfa.radio.model;


import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import media.toloka.rfa.radio.store.model.Store;

import java.util.Date;
import java.util.UUID;

@Data
@Entity
@Table(indexes = @Index(columnList = "uuid"))
public class Albumсover {
    @Id
    @GeneratedValue
    @Expose
    private Long id;
    @Expose
    private String uuid= UUID.randomUUID().toString();

    @Expose
    private String albumcoverfile;

    @Expose
    private String patch;

    @Expose
    private String albumcoverdateupload = new Date().toString();

    @Expose
    @Column(columnDefinition = "TEXT")
    private String description;

    @Expose
    private String storeuuid;

    @Expose
    @OneToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "store_id")
    private Store storeitem;

    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "clientdetail_id")
    private Clientdetail clientdetail;

}
