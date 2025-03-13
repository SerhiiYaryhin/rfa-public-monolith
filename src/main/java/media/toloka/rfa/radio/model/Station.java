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
    private String name;
    @Expose
    @Column(columnDefinition = "TEXT")
    private String description; // Опис радіостанції
    @Expose
    private String dbname;
    @Expose
    private boolean enable = false;
    @Expose
    private Date startdate;  // дата останього старту
    @Expose
    private Date createdate;  // дата створення
    @Expose
    private Date endworkdate;  // дата зупинки з панелі
    @Expose
    private Date lastpaydate;   // дата останьої оплати
    @Expose
    private Date lastchangedate;  //дата останьої зміни даних
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
    private String uuid;
    @Expose
    private String icecastname;
    @Expose
    private String icecastdescription;
    @Expose
    private String icecastsite;
    @Expose
    private String icecastgenre;
    @Expose
    private Boolean stationstate; // true if station runing state
    @Expose
    private String roomuuid = null;

//    private transient Contract contract;


    @ToString.Exclude
    @ManyToOne(optional = false, cascade = {CascadeType.ALL})
    @JoinColumn(name = "clientdetail_id")
    private Clientdetail clientdetail;

    @ToString.Exclude
    @ManyToOne(optional = true, cascade = {CascadeType.ALL})
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @ToString.Exclude
    @OneToMany(mappedBy = "station", fetch=FetchType.EAGER, cascade = {CascadeType.ALL})
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

