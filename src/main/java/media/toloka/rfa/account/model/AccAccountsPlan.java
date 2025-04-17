package media.toloka.rfa.account.model;
/// Довідник.
/// перелік наявних рахунків
import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import media.toloka.rfa.account.model.accEnum.AccActivePassive;
import media.toloka.rfa.account.model.polymorphing.AccBaseEntityDoc;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(
        indexes = {
                @Index(columnList = "uuid"),
                @Index(columnList = "acc"),
                @Index(columnList = "id")
        },
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"acc"}),
                @UniqueConstraint(columnNames = {"id"})
        }
)
public class AccAccountsPlan extends AccBaseEntityDoc {
//    @Id
//    @Expose
//    private String uuid = UUID.randomUUID().toString();
//
//    @Expose
//    @GeneratedValue
//    private Long id;

    @Expose
    private Long acc;
    @Expose
    private AccActivePassive accType; // пасивний, активний, астивно-пасивний
    @Expose
    private String accname;
    @Expose
    private String comment;


}
