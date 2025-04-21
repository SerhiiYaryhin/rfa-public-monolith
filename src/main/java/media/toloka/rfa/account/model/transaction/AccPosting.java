package media.toloka.rfa.account.model.transaction;
///  проводка.
///  повинна враховувати вид рах
import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import media.toloka.rfa.account.model.accEnum.EAccJobType;
import media.toloka.rfa.account.model.base.AccBaseDoc;
import media.toloka.rfa.account.model.base.AccBaseReference;
import media.toloka.rfa.account.model.base.AccBaseTransaction;
import media.toloka.rfa.account.model.accplan.AccAccountsPlan;

import java.math.BigDecimal;
import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
public class AccPosting extends AccBaseTransaction {
    @Expose
    private EAccJobType jobtype;
    @Expose
    private String jobTypeName;
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
    // аналітіка по дебиту
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "dtreflink1")
    private AccBaseReference dtRefLink1;
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "dtreflink2")
    private AccBaseReference dtRefLink2;
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "dtreflink3")
    private AccBaseReference dtRefLink3;

    @Expose
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "creditacc")
    private AccAccountsPlan creditacc;
    // аналітіка по кредиту
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "crreflink1")
    private AccBaseReference crRefLink1;
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "crreflink2")
    private AccBaseReference crRefLink2;
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "crreflink3")
    private AccBaseReference crRefLink3;

    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "transaction")
    private AccTransaction transaction;

}
