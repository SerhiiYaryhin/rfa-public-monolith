package media.toloka.rfa.radio.model;


//import jakarta.persistence.*;
import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import media.toloka.rfa.radio.model.enumerate.EClientAddressType;

import java.util.UUID;
//import javax.persistence.Id;

@Data
@ToString
@Entity
@Table(indexes = @Index(columnList = "uuid"))
public class Clientaddress {

    @Id
    @GeneratedValue
    @Expose
    private Long id;
    @Expose
    private String uuid= UUID.randomUUID().toString();
    @Expose
    private String shortaddress; // коротке найменування адреси
    @Expose
    private EClientAddressType userAddressType; // поштовий, офіційний, домашній
    @Expose
    private String street;
    @Expose
    private String buildnumber;
    @Expose
    private String korpus;
    @Expose
    private String appartment;
    @Expose
    private String cityname;
    @Expose
    private String area;
    @Expose
    private String region;
    @Expose
    private String country;
    @Expose
    private String zip;
    @Expose
    private String phone;
    @Expose
    private String comment;
    @Expose
    private Boolean mainaddress = false;
    @Expose
    private Boolean apruve = false;
//    private boolean cheked = false;

    @ToString.Exclude
    @ManyToOne(optional = false, cascade = {CascadeType.MERGE, CascadeType.REFRESH}) //, cascade = {CascadeType.ALL}
    @JoinColumn(name = "clientdetail_id")
    private Clientdetail clientdetail;

}
