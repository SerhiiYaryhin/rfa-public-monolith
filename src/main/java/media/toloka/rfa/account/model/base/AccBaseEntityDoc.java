package media.toloka.rfa.account.model.base;


import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import media.toloka.rfa.account.model.iface.PolymorphicTarget;
import media.toloka.rfa.radio.model.Clientdetail;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.util.Date;
import java.util.UUID;

// Базовий клас
@Data
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class AccBaseEntityDoc implements PolymorphicTarget {
    @Id
    @Expose
    @GeneratedValue
    @UuidGenerator
    private UUID uuid;
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
    private String docType; // тип документу
    @Expose
    @ManyToOne
    private Clientdetail customer; // клієнт
    @Expose
    @ManyToOne
    private Clientdetail operator; // оператор

//    @Override
//    public String getTypeCode() {
//        return null;
//    }
    @PrePersist
    public void setDefaults() {
        this.docType = this.getClass().getSimpleName(); ;
    }
}