package media.toloka.rfa.account.model;
// Базовий клас первичних документів послуг, товарів

import com.google.gson.annotations.Expose;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import media.toloka.rfa.radio.model.Clientdetail;

import java.math.BigDecimal;
import java.util.UUID;

public class AccDocument {
    @Id
    @Expose
    private String uuid;
    @Expose
    @GeneratedValue
    private Long id;

    private AccTemplateTransaction accTT;
    private Clientdetail cd;
    private Clientdetail operator;
    @Column(precision = 12, scale = 2)
    private BigDecimal total;
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
