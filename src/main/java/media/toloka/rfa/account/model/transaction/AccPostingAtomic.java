package media.toloka.rfa.account.model.transaction;
///  проводка.
///  повинна враховувати вид рах
import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import media.toloka.rfa.account.model.base.AccBaseReference;
import media.toloka.rfa.account.model.base.AccBaseTransaction;
import media.toloka.rfa.account.model.accplan.AccAccountsPlan;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
public class AccPostingAtomic extends AccBaseTransaction {

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

    // аналітіка по операції
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "reflink1")
    private AccBaseReference anomRefLink1;
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "reflink2")
    private AccBaseReference anomRefLink2;
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "reflink3")
    private AccBaseReference anomRefLink3;

    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "accPosting")
    private AccPosting accPosting;

}
