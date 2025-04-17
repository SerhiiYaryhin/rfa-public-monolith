package media.toloka.rfa.account.model.Documents;
// Рахунок на сплату

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import lombok.Data;
import media.toloka.rfa.account.model.AccGoods;
import media.toloka.rfa.account.model.polymorphing.AccBaseEntityDoc;
import media.toloka.rfa.account.model.polymorphing.iface.PolymorphicTarget;

import java.util.List;


@Data
@Entity
public class AccInvoiceDocument  extends AccBaseEntityDoc {

    @OneToMany// (mappedBy = "transaction", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    List<AccGoods> goods;

    @Override
    public String getTypeCode() {
        return "INVOICE";
    }
}
