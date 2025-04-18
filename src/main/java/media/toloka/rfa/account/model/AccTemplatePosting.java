package media.toloka.rfa.account.model;
///  Типова проводка (posting) що міститься в типовій операції (transaction)
import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import media.toloka.rfa.account.model.polymorphing.AccBaseEntityDoc;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.util.Date;
import java.util.UUID;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
public class AccTemplatePosting extends AccBaseEntityDoc {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "uuid", columnDefinition = "uuid", updatable = false, nullable = false)
//    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
//    @Type("uuid-char")
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
    private Long debit;
    @Expose
    private Long credit;
    @Expose
    private String name;
    @Expose
    private String comment;

    @Expose
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "debitacc")
    private AccAccountsPlan debitacc;

    @Expose
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "creditacc")
    private AccAccountsPlan creditacc;

    @Expose
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "transaction") // <-- SQL-стовпець
    private AccTemplateTransaction transaction;

}
