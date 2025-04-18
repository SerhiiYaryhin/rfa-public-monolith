package media.toloka.rfa.account.model;
/// Довідник.
/// перелік наявних рахунків
import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import media.toloka.rfa.account.model.accEnum.EAccActivePassive;
import media.toloka.rfa.account.model.polymorphing.AccBaseEntityDoc;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.util.Date;
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
    @Id
    @GeneratedValue
    @UuidGenerator
    @Expose
    private UUID uuid;
    @Expose
    private Long id;
    @Expose
    private Long docNumber; // Номер документа
    @Expose
    @LastModifiedDate
    private Date docoperation; // дата проводки
    @Expose
    @CreatedDate
    private Date docCreate; // дата документа
    @Expose
    private String docType = getTypeCode(); // тип документу

    // =====================================================

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
