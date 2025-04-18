package media.toloka.rfa.account.model;
///  проводка.
///  повинна враховувати вид рах
import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import media.toloka.rfa.account.model.base.AccBaseTransaction;

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

    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "accPosting")
    private AccPosting accPosting;

}
