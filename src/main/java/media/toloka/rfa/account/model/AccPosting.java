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

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
public class AccPosting extends AccBaseEntityDoc {

    @Expose
    private EAccJobType jobtype;
    @Expose
    @Column(precision = 12, scale = 2)
    private BigDecimal value;

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
    private AccTransaction operationSet;

}
