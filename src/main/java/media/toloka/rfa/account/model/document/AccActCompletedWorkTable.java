package media.toloka.rfa.account.model.document;
// виконана робота
// конкретна робота, що була виконана. Наприклад - текст -> голос, робота радіостанції тощо

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import media.toloka.rfa.account.model.accEnum.EAccJobType;
import media.toloka.rfa.account.model.base.AccBaseDoc;
import media.toloka.rfa.account.model.iface.PolymorphicTarget;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

/// акт виконаних робіт
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
public class AccActCompletedWorkTable extends AccBaseDoc implements PolymorphicTarget {


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

}
