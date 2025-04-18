package media.toloka.rfa.account.model.document;
/// Акт Виконаних робіт
/// формується на підставі окремих виконаних робіт
import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import media.toloka.rfa.account.model.AccAccountsPlan;
import media.toloka.rfa.account.model.base.AccBaseDoc;
import media.toloka.rfa.account.model.iface.PolymorphicTarget;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.util.Date;
import java.util.List;
import java.util.UUID;


@EqualsAndHashCode(callSuper = false)
@Data
@Entity
public class AccActCompletedWorkDocument extends AccBaseDoc implements PolymorphicTarget {


    @Expose
    @ToString.Exclude
    @OneToMany(mappedBy = "act", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AccActCompletedWorkTable> completedWorkList;
// типова транзакція
    @Expose
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "templatetransaction")
    private AccAccountsPlan templatetransaction;


//    @Override
//    public String getTypeCode() {
//        return "ACTCOMPLETEWORK";
//    }
}
