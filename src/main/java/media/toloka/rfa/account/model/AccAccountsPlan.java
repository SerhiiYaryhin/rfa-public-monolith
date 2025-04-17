package media.toloka.rfa.account.model;
/// Довідник.
/// перелік наявних рахунків
import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import media.toloka.rfa.account.model.accEnum.EAccActivePassive;
import media.toloka.rfa.account.model.polymorphing.AccBaseEntityDoc;

import java.util.UUID;

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

    @Expose
    private Integer acc;
    @Expose
    private EAccActivePassive accType; // пасивний, активний, астивно-пасивний
    @Expose
    private String accName;
    @Expose
    private String comment;

//    @PrePersist
//    @Override
//    public void generateUUID() {
//        if (this.uuid == null) {
//            this.uuid = UUID.randomUUID().toString();
//        }
//        if (this.id == null) {
//            this.id = System.currentTimeMillis(); // Метод для генерації унікального ID
//        }
//    }
}
