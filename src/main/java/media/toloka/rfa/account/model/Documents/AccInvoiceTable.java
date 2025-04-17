package media.toloka.rfa.account.model.Documents;
// Рахунок на сплату

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import media.toloka.rfa.account.model.AccGoodsReference;
import media.toloka.rfa.account.model.AccMeasurementReference;
import media.toloka.rfa.account.model.polymorphing.AccBaseEntityDoc;

import java.math.BigDecimal;
import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
public class AccInvoiceTable extends AccBaseEntityDoc {

    @Expose
    @OneToMany  // (mappedBy = "transaction", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AccGoodsReference> accGoodReferences;

    @Expose
    @ManyToOne
    private AccMeasurementReference measurement;
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


    @Override
    public String getTypeCode() {
        return "INVOICE";
    }
}
