package media.toloka.rfa.account.model.polymorphing;


import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import media.toloka.rfa.account.model.AccTemplateTransaction;
import media.toloka.rfa.account.model.polymorphing.iface.PolymorphicTarget;
import media.toloka.rfa.radio.model.Clientdetail;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.util.Date;
import java.util.UUID;

// Базовий клас
@Data
@MappedSuperclass
public abstract class AccBaseEntityDoc implements PolymorphicTarget{
    @Id
    @Expose
    private String uuid;
    @Expose
//    @GeneratedValue
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
    @Expose
    @ManyToOne
    private Clientdetail customer; // клієнт
    @Expose
    @ManyToOne
    private Clientdetail operator; // оператор

// прибрав, бо це є не у всіх таблицях. Потрібно тільки в документах
// Виніс у ті таблиці, в яких це потрібно
//    @Expose
//    @ManyToOne
//    private AccTemplateTransaction accTemplateTransaction = null; // типова операція

    @Override
    public String getTypeCode() {
        return null;
    }

    @PrePersist
    public void generateUUID() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
        if (this.id == null) {
            this.id = System.currentTimeMillis(); // Метод для генерації унікального ID
        }
    }
}