package media.toloka.rfa.account.model;
// Базовий клас первичних документів послуг, товарів

import com.google.gson.annotations.Expose;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import media.toloka.rfa.account.model.polymorphing.AccBaseEntityDoc;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

public class AccDocument extends AccBaseEntityDoc {
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

    @Column(precision = 12, scale = 2)
    private BigDecimal total;

}
