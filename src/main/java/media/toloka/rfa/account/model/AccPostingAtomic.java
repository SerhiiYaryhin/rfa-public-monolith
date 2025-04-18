package media.toloka.rfa.account.model;
///  проводка.
///  повинна враховувати вид рах
import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import media.toloka.rfa.account.model.accEnum.EAccJobType;
import media.toloka.rfa.account.model.polymorphing.AccBaseEntityDoc;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
public class AccPostingAtomic extends AccBaseEntityDoc {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Expose
    private UUID uuid;
    @Expose
//    @GeneratedValue
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    @Column(precision = 12, scale = 2)
    private BigDecimal value;

    @Expose
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "acc")
    private AccAccountsPlan acc;

    @Expose
    @Column(precision = 12, scale = 2)
    private BigDecimal debit;
    @Expose
    @Column(precision = 12, scale = 2)
    private BigDecimal credit;

    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "accPosting")
    private AccPosting accPosting;


}
