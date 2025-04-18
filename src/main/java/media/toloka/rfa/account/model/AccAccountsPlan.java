package media.toloka.rfa.account.model;
/// Довідник.
/// перелік наявних рахунків

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import media.toloka.rfa.account.model.accEnum.EAccActivePassive;
import media.toloka.rfa.account.model.base.AccBaseTransaction;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@DiscriminatorValue("AccAccountsPlan")
@Table(
        indexes = {
//                @Index(columnList = "uuid"),
                @Index(columnList = "acc"),
//                @Index(columnList = "id")
        },
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"acc"}),
//                @UniqueConstraint(columnNames = {"id"})
        }
)
public class AccAccountsPlan extends AccBaseTransaction {

    // =====================================================

    @Expose
    private Integer acc;
    @Expose
    private EAccActivePassive accType; // пасивний, активний, астивно-пасивний
    @Expose
    private String accName;
    @Expose
    private String comment;

}



