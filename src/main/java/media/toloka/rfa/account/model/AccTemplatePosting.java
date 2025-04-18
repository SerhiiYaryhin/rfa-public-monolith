package media.toloka.rfa.account.model;
///  Типова проводка (posting) що міститься в типовій операції (transaction)
import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import media.toloka.rfa.account.model.base.AccBaseTransaction;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
public class AccTemplatePosting extends AccBaseTransaction {
//    @Id
//    @GeneratedValue
//    @UuidGenerator
//    @Column(name = "uuid", columnDefinition = "uuid", updatable = false, nullable = false)
//    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
//    @Type("uuid-char")


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


//    @Expose
//    private String docType;
//    @Expose
//    private Long id;
//    @PrePersist
//    public void setDefaults() {
//        this.docType = getTypeCode();
//        if (this.id == null) this.id = System.currentTimeMillis(); // Метод для генерації унікального ID
//    }
////    @Override
//    public String getTypeCode() {
//        String className = this.getClass().getSimpleName(); ;
//        return className;
//    }
}
