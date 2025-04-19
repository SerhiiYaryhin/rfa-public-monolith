package media.toloka.rfa.account.model.referens;
// // Базовий клас первичних документів послуг, товарів

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import media.toloka.rfa.account.model.base.AccBaseDoc;
import media.toloka.rfa.account.model.base.AccBaseReference;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
public class AccGoodsReference extends AccBaseReference {

    @Expose
    private String name;
    @Expose
    @Column(columnDefinition = "TEXT")
    private String comments;
    @Expose
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "measurement")
    private AccMeasurementReference measurement;
    @Expose
    @Column(precision = 12, scale = 3)
    private BigDecimal quantity;
    @Expose
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

}
