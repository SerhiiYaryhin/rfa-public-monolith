package media.toloka.rfa.radio.model;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import media.toloka.rfa.radio.model.enumerate.EServerState;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.security.model.Users;


import java.util.*;

@Data
@Entity
@Table(indexes = @Index(columnList = "uuid"))
public class Station {

    @Id
    @GeneratedValue
    @Expose
    private Long id;
    @Expose
    private EServerState serverState; // user record state (banned, ... etc)
    @Expose
    private String name; // назва станції
    @Expose
    @Column(columnDefinition = "TEXT")
    private String description; // Опис радіостанції
    @Expose
    private String dbname; // TODO: PROPOSAL -> dbName (Keep 'dbname' in DB/JSON for backward compatibility)
    @Expose
    private String purchaseditem = null; // TODO: PROPOSAL -> purchasedItem
    @Expose
    private boolean enable = false; // станцію можна запускати
    @Expose
    private Date startdate;  // TODO: PROPOSAL -> startDate
    @Expose
    private Date createdate;  // TODO: PROPOSAL -> createDate
    @Expose
    private Date endworkdate;  // TODO: PROPOSAL -> endWorkDate
    @Expose
    private Date lastpaydate;   // TODO: PROPOSAL -> lastPayDate
    @Expose
    private Date lastchangedate;  // TODO: PROPOSAL -> lastChangeDate
    @Expose
    public Integer guiport; // port gui server
    @Expose
    public String guiserver; // address server allocate gui Libretime
    @Expose
    public String radioserver = null; // address server allocate gui Libretime
    @Expose
    public Integer main; // port main
    @Expose
    public Integer show; // port show
    @Expose
    private String uuid; // заповнюється десь в програмі при створенні станції. Чи можна тут заповнювати?
    @Expose
    private String icecastname; // TODO: PROPOSAL -> icecastName
    @Expose
    private String icecastdescription; // TODO: PROPOSAL -> icecastDescription
    @Expose
    private String icecastsite; // TODO: PROPOSAL -> icecastSite
    @Expose
    private String icecastgenre; // TODO: PROPOSAL -> icecastGenre
    //
    @Expose
    private Boolean stationstate; // TODO: PROPOSAL -> isRunning (CRITICAL: used in JSON/RabbitMQ)
    @Expose
    private String roomuuid = null; // UUID кімнати для онлайн мовлення TODO: PROPOSAL -> roomUuid
    @Expose
    private String toradiouser; // TODO: PROPOSAL -> toRadioUser
    @Expose
    @Column(columnDefinition = "TEXT")
    private String toradiopassword; // TODO: PROPOSAL -> toRadioPassword

//    private transient Contract contract;


    @ToString.Exclude
//    @ManyToOne(optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.REFRESH}) //  PERSIST видалено!
    @JoinColumn(name = "clientdetail_id")
    private Clientdetail clientdetail;

    @ToString.Exclude
    @ManyToOne(optional = true, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @ToString.Exclude
    @OneToMany(mappedBy = "station", fetch=FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private List<Poolport> ports = new ArrayList<>();

    @ToString.Exclude
    @OneToOne(cascade = {CascadeType.ALL})
    private Store stationlogo;


    public Station() {
        this.uuid= UUID.randomUUID().toString();
        this.name=this.uuid;
        this.createdate=new Date();
        this.lastchangedate=new Date();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 2);
        this.endworkdate= cal.getTime();
        // Date.before(), Date.after() and Date.equals()
        this.stationstate = false;

    }
}

