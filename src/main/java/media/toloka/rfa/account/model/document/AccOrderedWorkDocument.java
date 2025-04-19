package media.toloka.rfa.account.model.document;
// замовлення на виконання роботи

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import media.toloka.rfa.account.model.accplan.AccAccountsPlan;
import media.toloka.rfa.account.model.base.AccBaseDoc;
import media.toloka.rfa.radio.model.Clientdetail;


import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
public class AccOrderedWorkDocument  extends AccBaseDoc {

//    @Expose
//    @ManyToOne
//    private AccMeasurementReference measurement;
    @Expose
    @Column(precision = 12, scale = 3)
    private BigDecimal quantity;
    @Expose
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    //
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
}
