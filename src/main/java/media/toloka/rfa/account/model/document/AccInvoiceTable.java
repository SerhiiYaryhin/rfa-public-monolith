package media.toloka.rfa.account.model.document;
// Рахунок на сплату

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import media.toloka.rfa.account.model.base.AccBaseDoc;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
public class AccInvoiceTable extends AccBaseDoc {

//    @Expose
//    @OneToMany  // (mappedBy = "transaction", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<AccGoodsReference> accGoodReferences;
//
//    @Expose
//    @ManyToOne
//    private AccMeasurementReference measurement;
    @Expose
    @Column(precision = 12, scale = 3)
    private BigDecimal quantity;
    @Expose
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Expose
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "invoice") // <-- SQL-стовпець
    private AccInvoiceDocument invoice;

}
