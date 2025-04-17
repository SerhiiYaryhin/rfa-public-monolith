package media.toloka.rfa.account.model;
///  Типова проводка (posting) що міститься в типовій операції (transaction)
import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.util.UUID;

@Data
@Entity
public class AccTemplatePosting {
    @Id
    @Expose
    private String uuid;
    @Expose
    private Long id;

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
