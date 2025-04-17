package media.toloka.rfa.account.model.Documents;
// виконана робота
// конкретна робота, що була виконана. Наприклад - текст -> голос, робота радіостанції тощо

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import media.toloka.rfa.account.model.accEnum.EAccJobType;
import media.toloka.rfa.account.model.polymorphing.AccBaseEntityDoc;
import media.toloka.rfa.account.model.polymorphing.iface.PolymorphicTarget;
import media.toloka.rfa.radio.model.Clientdetail;

import java.math.BigDecimal;
/// акт виконаних робіт
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
public class AccActCompletedWorkTable extends AccBaseEntityDoc implements PolymorphicTarget {

    @Expose
    private EAccJobType jobType;
    @Column(precision = 12, scale = 3)
    private BigDecimal quantity;
// документ з якого увійшли роботи
    @Expose
    private String documentType = null;
    @Expose
    private String documentUuid = null;

    @Expose
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "act")
    private AccActCompletedWorkDocument act;

    @Override
    public String getTypeCode() {
        return "COMPLETEWORK";
    }
}
