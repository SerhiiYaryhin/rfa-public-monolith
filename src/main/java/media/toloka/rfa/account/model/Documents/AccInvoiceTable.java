package media.toloka.rfa.account.model.Documents;
// Рахунок на сплату

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import media.toloka.rfa.account.model.AccGoodsReference;
import media.toloka.rfa.account.model.AccMeasurementReference;
import media.toloka.rfa.account.model.polymorphing.AccBaseEntityDoc;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
public class AccInvoiceTable extends AccBaseEntityDoc {
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
    @OneToMany  // (mappedBy = "transaction", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AccGoodsReference> accGoodReferences;

    @Expose
    @ManyToOne
    private AccMeasurementReference measurement;
    @Expose
    @Column(precision = 12, scale = 3)
    private BigDecimal quantity;
    @Expose
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Expose
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "invoice") // <-- SQL-стовпець
    private AccInvoiceDocument invoice;


    @Override
    public String getTypeCode() {
        return "INVOICE";
    }
}
