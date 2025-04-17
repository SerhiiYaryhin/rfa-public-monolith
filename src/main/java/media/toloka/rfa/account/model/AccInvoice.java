package media.toloka.rfa.account.model;

import com.google.gson.annotations.Expose;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;

import java.math.BigDecimal;
import java.util.UUID;

public class AccInvoice extends FatherDocuments  {
    @Id
    @Expose
    private String uuid;
    @Expose
    @GeneratedValue
    private Long id;

    @Column(precision = 12, scale = 2)
    private BigDecimal total;

    @Expose
    private Long docNumber; // Номер документа
// наш Товар
//    private List<AccGoods> service;

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
