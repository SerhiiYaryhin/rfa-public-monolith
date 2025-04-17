package media.toloka.rfa.account.model;
///  Типова проводка (posting) що міститься в типовій операції (transaction)
import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import media.toloka.rfa.account.model.polymorphing.AccBaseEntityDoc;

import java.util.UUID;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
public class AccTemplatePosting extends AccBaseEntityDoc {

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
