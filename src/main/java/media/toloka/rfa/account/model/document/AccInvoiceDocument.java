package media.toloka.rfa.account.model.document;
// Рахунок на сплату

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import media.toloka.rfa.account.model.accplan.AccAccountsPlan;
import media.toloka.rfa.account.model.base.AccBaseDoc;

import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
public class AccInvoiceDocument  extends AccBaseDoc {

    @Expose
    @ToString.Exclude
    @OneToMany(mappedBy = "invoice", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AccInvoiceTable> invoiceTableList;

    // типова транзакція
    @Expose
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "templatetransaction")
    private AccAccountsPlan templatetransaction;

}
