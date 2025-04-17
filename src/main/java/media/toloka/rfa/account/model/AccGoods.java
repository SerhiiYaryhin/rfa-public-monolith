package media.toloka.rfa.account.model;
// // Базовий клас первичних документів послуг, товарів

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import media.toloka.rfa.account.model.polymorphing.AccBaseEntityDoc;

import java.math.BigDecimal;
import java.util.UUID;

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
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

}
