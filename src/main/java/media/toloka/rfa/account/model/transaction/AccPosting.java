package media.toloka.rfa.account.model.transaction;
///  проводка.
///  повинна враховувати вид рах
import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import media.toloka.rfa.account.model.accEnum.EAccJobType;
import media.toloka.rfa.account.model.base.AccBaseTransaction;
import media.toloka.rfa.account.model.accplan.AccAccountsPlan;

import java.math.BigDecimal;
import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
public class AccPosting extends AccBaseTransaction {
//    @Id
//    @GeneratedValue
//    @UuidGenerator
//    @Expose
//    private UUID uuid;
//    @Expose
//    private Long id;
//    @Expose
//    private Long docNumber; // Номер документа
//    @Expose
//    @LastModifiedDate
//    private Date docoperation; // дата проводки
//    @Expose
//    @CreatedDate
//    private Date docCreate; // дата документа
//    @Expose
//    private String docType = getTypeCode(); // тип документу

    // =====================================================

    @Expose
    private EAccJobType jobtype;
    @Expose
    @Column(precision = 12, scale = 2)
    private BigDecimal value;
    @Expose
    @Column(precision = 12, scale = 3)
    private BigDecimal quantity;

    @OneToMany(mappedBy = "accPosting", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AccPostingAtomic> accPostingAtomicList;

    @Expose
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "debitacc")
    private AccAccountsPlan debitacc;

    @Expose
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "creditacc")
    private AccAccountsPlan creditacc;

    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "transaction")
    private AccTransaction transaction;

}
