package media.toloka.rfa.account.model.Documents;
// надходження грошей на рахунок

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import media.toloka.rfa.account.model.AccMeasurementReference;
import media.toloka.rfa.account.model.polymorphing.AccBaseEntityDoc;
import media.toloka.rfa.account.model.polymorphing.iface.PolymorphicTarget;
import media.toloka.rfa.radio.model.Clientdetail;

import java.math.BigDecimal;
import java.util.Date;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
public class AccInBankStatementTable extends AccBaseEntityDoc {

    @Expose
    private Date datePosting;
    @Expose
    @Column(precision = 12, scale = 2)
    private BigDecimal value;
    @Expose
    private String documentType = null;
    @Expose
    private String documentUuid = null;
    @Expose
    private String comment = null;
    @Expose
    @ManyToOne
    private Clientdetail payer;

    @Expose
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "statementdocument") // <-- SQL-стовпець
    private AccInBankStatementDocument statementdocument;

    @Override
    public String getTypeCode() {
        return "BankStatement";
    }
}
