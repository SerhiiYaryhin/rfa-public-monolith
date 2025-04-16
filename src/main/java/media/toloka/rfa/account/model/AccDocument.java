package media.toloka.rfa.account.model;
// Базовий клас первичних документів послуг, товарів

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import media.toloka.rfa.account.model.Documents.AccInFlowToBankDocument;
import media.toloka.rfa.account.model.Documents.AccOrderedWorkDocument;
import media.toloka.rfa.account.model.polymorphing.AccBaseEntityDoc;
import media.toloka.rfa.account.model.polymorphing.iface.PolymorphicTarget;
import org.hibernate.annotations.Any;
//import org.hibernate.annotations.AnyMetaDef;

import java.util.UUID;

//@AnyMetaDef(name = "TargetMetaDef", metaType = "string", idType = "long",
//        metaValues = {
//                @MetaValue(targetEntity = AccInFlowToBankDocument.class, value = "FLOWTOBANK"),
//                @MetaValue(targetEntity = AccInvoice.class, value = "INVOICE"),
//                @MetaValue(targetEntity = AccOrderedWorkDocument.class, value = "ORDERWORK")
//        }
//)
public class AccDocument extends AccBaseEntityDoc {
//    @Id
//    @Expose
//    private String uuid;
//    @Expose
//    @GeneratedValue
//    private Long id;

    private String action;

    @Column(name = "target_type")
    private String targetType;

//    @Any(metaDef = "TargetMetaDef", metaColumn = @Column(name = "target_type"))
//    @JoinColumn(name = "target_id")
//    private PolymorphicTarget target;

//    @Column(precision = 12, scale = 2)
//    private BigDecimal total;
// наш Товар
//    private List<AccGoods> service;

//    @PrePersist
//    public void generateUUID() {
//        if (this.uuid == null) {
//            uuid = UUID.randomUUID().toString();
//        }
//        if (this.id == null) {
//            this.id = System.currentTimeMillis(); // Метод для генерації унікального ID
//        }
//    }

}
