package media.toloka.rfa.account.model.Documents;
// виконана робота
// конкретна робота, що була виконана. Наприклад - текст -> голос, робота радіостанції тощо

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import media.toloka.rfa.account.model.AccAccountsPlan;
import media.toloka.rfa.account.model.accEnum.EAccJobType;
import media.toloka.rfa.account.model.polymorphing.AccBaseEntityDoc;
import media.toloka.rfa.account.model.polymorphing.iface.PolymorphicTarget;
import media.toloka.rfa.radio.model.Clientdetail;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
public class AccCompletedWorkDocument extends AccBaseEntityDoc implements PolymorphicTarget {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Expose
    private UUID uuid;
    @Expose
    private Long id;
    @Expose
    private Long docNumber; // Номер документа
    @Expose
    @LastModifiedDate
    private Date docoperation; // дата проводки
    @Expose
    @CreatedDate
    private Date docCreate; // дата документа
    @Expose
    private String docType = getTypeCode(); // тип документу

    // =====================================================

    @Expose
    private EAccJobType jobType;

    @Column(precision = 12, scale = 3)
    private BigDecimal quantity;

    @Expose
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "customer")
    private Clientdetail customer;

    // типова транзакція
    @Expose
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "templatetransaction")
    private AccAccountsPlan templatetransaction;

    @Override
    public String getTypeCode() {
        return "COMPLETEWORK";
    }
}
