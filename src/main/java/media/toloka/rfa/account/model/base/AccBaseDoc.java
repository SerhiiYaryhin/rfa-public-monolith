package media.toloka.rfa.account.model.base;


import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import media.toloka.rfa.account.model.iface.PolymorphicTarget;
import media.toloka.rfa.radio.model.Clientdetail;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;
import java.util.UUID;

// Базовий клас
@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
public abstract class AccBaseDoc implements PolymorphicTarget {
    @Id
    @Expose
    @GeneratedValue
    @UuidGenerator
    private UUID uuid;
    @Expose
//    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "doc_number_seq")
//    @SequenceGenerator(name = "doc_number_seq", sequenceName = "doc_number_seq", allocationSize = 8)
    @SequenceGenerator(
            name = "doc_number_seq",
            sequenceName = "doc_number_seq",
            allocationSize = 8 // або 8, якщо кешування ок
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "doc_number_seq"
    )
    private Long docNumber; // Номер документа
    @Expose
    @LastModifiedDate
    private Date docoperation; // дата проводки
    @Expose
    @CreatedDate
    private Date docCreate; // дата документа
//    @Expose
//    private String dtype; // тип документу
    @Expose
    @ManyToOne
    private Clientdetail customer; // клієнт
    @Expose
    @ManyToOne
    private Clientdetail operator; // оператор

    @Transient
    public String getActualType() {
        return this.getClass().getSimpleName();
    }

    @PrePersist
    @Override
    public void generateDocNum() {
        this.docNumber = System.currentTimeMillis() / 1000;
    }
}