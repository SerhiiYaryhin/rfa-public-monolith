package media.toloka.rfa.account.model.polymorphing;


import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import media.toloka.rfa.account.model.AccTemplateTransaction;
import media.toloka.rfa.account.model.polymorphing.iface.PolymorphicTarget;
import media.toloka.rfa.radio.model.Clientdetail;

import java.util.Date;
import java.util.UUID;

// Базовий клас
@MappedSuperclass
public abstract class AccBaseEntityDoc {
    @Id
    @Expose
    private String uuid;
    @Expose
    @GeneratedValue
    private Long id;
    @Expose
    private Long docNumber; // Номер документа
    @Expose
    private Date docoperation; // дата проводки
    @Expose
    private Date docCreate; // дата документа
    @Expose
    private String name; // найменування документу
    @Expose
    @ManyToOne
    private Clientdetail client; // клієнт
    @Expose
    @ManyToOne
    private Clientdetail operator; // оператор
    @Expose
    @ManyToOne
    private AccTemplateTransaction accTT; // типова операція



//    @PrePersist
//    public void generateUUID() {
//        if (uuid == null) {
//            uuid = UUID.randomUUID().toString();
//        }
//        if (this.id == null) {
//            this.id = System.currentTimeMillis(); // Метод для генерації унікального ID
//        }
//    }
}