package media.toloka.rfa.radio.model;

//import com.google.gson.annotations.Expose;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import media.toloka.rfa.radio.model.enumerate.EContractStatus;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

//import static media.toloka.rfa.radio.contract.model.EContractStatus.CONTRACT_FREE;

@Data
@Entity
@Table(indexes = @Index(columnList = "uuid"))
public class Contract {

    @Id
    @GeneratedValue
    @Column(name="id")
    @Expose
    private Long id;
    @Expose
    private EContractStatus contractStatus;
    @Expose
    private String number;
    @Expose
    private String uuid;
    @Expose
    private Date createDate;
    @Expose
    private Date lastPayDate;
    @Expose
    private String contractname;
    @Expose
    private String usercomment;

    @ToString.Exclude
    @ManyToOne(optional = false, fetch=FetchType.EAGER, cascade = {CascadeType.ALL})
    @JoinColumn(name = "clientdetail_id")
    private Clientdetail clientdetail;

    @ToString.Exclude
    @OneToMany(mappedBy = "contract", fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
    private List<Station> stationList = new ArrayList<>();


    //    @ElementCollection
//    @OneToMany(cascade = CascadeType.ALL)
//    private List<Station> listStation;

//    @OneToMany(cascade = CascadeType.ALL)
//    private List<Radio> radios;

//    @OneToMany(cascade = CascadeType.ALL)
//    private List<History> history;


    public Contract() {
        this.contractStatus         = EContractStatus.CONTRACT_FREE;
        this.number                 = UUID.randomUUID().toString();
        this.uuid                   = UUID.randomUUID().toString();
        this.createDate             = new Date();
        this.lastPayDate            = null;
    }

}
