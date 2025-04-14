package media.toloka.rfa.account.model;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import media.toloka.rfa.radio.model.Clientdetail;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@Data
@Entity
@Table(indexes = {@Index(columnList = "uuid"), @Index(columnList = "id")})
public class AccCashFlow {
    @Id
    @Expose
    private String uuid;
    @Expose
    @GeneratedValue
    private Long id;

    @Expose
    private Date operationdate;

    @Expose
    @Column(precision = 12, scale = 2)
    private BigDecimal value;

    @Expose
    private EAccJobType jobtype;

    @Expose
    @Column(columnDefinition = "TEXT")
    private String comments;

    @Expose
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "acc_uuid")
    private AccAccounts acc;

    @Expose
    @ToString.Exclude
    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "accoperation")
    private AccOperation accoperation;

    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "clientdetail_id")
    private Clientdetail client;

    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "operator_id")
    private Clientdetail operator;

    @PrePersist
    public void generateUUID() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        if (this.id == null) {
            this.id = System.currentTimeMillis(); // Метод для генерації унікального ID
        }
    }

}
