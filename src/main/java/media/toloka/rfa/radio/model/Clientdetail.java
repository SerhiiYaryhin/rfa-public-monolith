package media.toloka.rfa.radio.model;

//import jakarta.persistence.*;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import media.toloka.rfa.media.messanger.model.MessageRoom;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.security.model.Users;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(indexes = @Index(columnList = "uuid"))
public class Clientdetail {
    @Id
    @GeneratedValue
    @Expose
    private Long id;
    @Expose
    private String custname;
    @Expose
    private String custsurname;
    @Expose
    private String firmname;
    @Expose
    private String uuid;
    @Expose
    private String telegramuser = null;
    @Expose
    private String telegramuserchatid = null;
    @Expose
    private Boolean confirminfo = false;
    @Expose
    private Date confirmDate;
    @Expose
    private Date createdate;
    @Expose
    @Column(columnDefinition = "TEXT")
    private String comments;
    @Expose
    private Double account = 0.0;

    //    @Expose
//    @ElementCollection
    @ToString.Exclude
    @OneToOne(cascade = {CascadeType.ALL})
    private Store profilephoto;

    @ToString.Exclude
    @OneToMany(mappedBy = "clientdetail", fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
    //,cascade = {CascadeType.ALL},fetch= FetchType.EAGER)
    private List<Clientaddress> clientaddressList = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "clientdetail", fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
    //,cascade = {CascadeType.ALL},fetch= FetchType.EAGER)
    private List<Contract> contractList = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "clientdetail", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<History> historyList = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "clientdetail", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<Documents> documentslist = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "clientdetail", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<Station> stationList = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "clientdetail", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<Album> albumList = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "clientdetail", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<Track> trackList = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "clientdetail", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<Post> postList = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "clientdetail", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<Albumсover> albumсoverList = new ArrayList<>();

//    @ToString.Exclude
//    @OneToMany(mappedBy = "clientdetail", fetch=FetchType.EAGER, cascade = CascadeType.MERGE)
//    @OneToMany(mappedBy = "clientdetail", fetch=FetchType.EAGER, cascade = CascadeType.MERGE)
//    @LazyCollection(LazyCollectionOption.TRUE)
//    private List<Store> filesinstore = new ArrayList<>();

    @ToString.Exclude
    @ManyToMany
    private List<MessageRoom> roomList = new ArrayList<>();

    @ToString.Exclude
    @OneToOne(cascade = {CascadeType.ALL})
    private Users user;

    public Clientdetail() {
        this.uuid = UUID.randomUUID().toString();
        this.createdate = new Date();
        this.confirminfo = false;
        this.account = 0.0;
    }

}

