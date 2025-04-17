package media.toloka.rfa.account.model;
// Базовий клас первичних документів послуг, товарів

import com.google.gson.annotations.Expose;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import media.toloka.rfa.account.model.polymorphing.AccBaseEntityDoc;

import java.math.BigDecimal;
import java.util.UUID;

public class AccDocument extends AccBaseEntityDoc {

    @Column(precision = 12, scale = 2)
    private BigDecimal total;

}
