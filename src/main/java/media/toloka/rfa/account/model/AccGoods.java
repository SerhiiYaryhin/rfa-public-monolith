package media.toloka.rfa.account.model;
// // Базовий клас первичних документів послуг, товарів

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import media.toloka.rfa.account.model.polymorphing.AccBaseEntityDoc;

import java.math.BigDecimal;
import java.util.UUID;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
public class AccGoods extends AccBaseEntityDoc {
//    @Id
//    @Expose
//    private String uuid;
//    @Expose
//    @GeneratedValue
//    private Long id;

    @Expose
    private String name;
    @Expose
    @Column(columnDefinition = "TEXT")
    private String comments;
    @Expose
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "measurement")
    private AccReferenceMeasurement measurement;
    @Expose
    @Column(precision = 12, scale = 3)
    private BigDecimal quantity;
    @Expose
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

}
