package media.toloka.rfa.account.model;
// // Базовий клас первичних документів послуг, товарів

import com.google.gson.annotations.Expose;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;

import java.math.BigDecimal;
import java.util.UUID;

public class AccGoods {
    @Id
    @Expose
    private String uuid;
    @Expose
    @GeneratedValue
    private Long id;

    @Expose
    private String name;
    @Expose
    @Column(columnDefinition = "TEXT")
    private String comments;
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

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
