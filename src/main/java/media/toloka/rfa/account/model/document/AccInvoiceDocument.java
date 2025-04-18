package media.toloka.rfa.account.model.document;
// Рахунок на сплату

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import media.toloka.rfa.account.model.AccAccountsPlan;
import media.toloka.rfa.account.model.base.AccBaseDoc;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.util.Date;
import java.util.List;
import java.util.UUID;

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
