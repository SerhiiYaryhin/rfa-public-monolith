package media.toloka.rfa.account.model;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import media.toloka.rfa.account.model.polymorphing.iface.PolymorphicTarget;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Entity
public class AccInvoice implements PolymorphicTarget {
    @Id
    @Expose
    private String uuid;
    @Expose
    @GeneratedValue
    private Long id;
    @Expose
    private Long docNumber; // Номер документа

    @Column(precision = 12, scale = 2)
    private BigDecimal total; // Сума документу


// наш Товар
//    private List<AccGoods> service;

    @Override
    public String getTypeCode() {
        return "INVOICE";
    }

    @PrePersist
    public void generateUUID() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        if (this.id == null) {
            this.id = System.currentTimeMillis(); // Метод для генерації унікального ID
        }
    }

}
