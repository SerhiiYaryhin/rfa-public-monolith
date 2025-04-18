package media.toloka.rfa.account.model.document;
// надходження грошей на рахунок

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import media.toloka.rfa.account.model.base.AccBaseDoc;
import media.toloka.rfa.radio.model.Clientdetail;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
public class AccInBankStatementTable extends AccBaseDoc {

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

}
