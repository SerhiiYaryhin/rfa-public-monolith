package media.toloka.rfa.account.model.document;
/// Банківська виписка
/// надходження грошей на рахунок

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import media.toloka.rfa.account.model.accplan.AccAccountsPlan;
import media.toloka.rfa.account.model.base.AccBaseDoc;
import media.toloka.rfa.account.model.iface.PolymorphicTarget;

import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
public class AccInBankStatementDocument extends AccBaseDoc implements PolymorphicTarget { //implements PolymorphicTarget {

    @Expose
    @OneToMany(mappedBy = "statementdocument", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AccInBankStatementTable> statementTableList;
    // типова транзакція
    @Expose
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "templatetransaction")
    private AccAccountsPlan templatetransaction;

}
