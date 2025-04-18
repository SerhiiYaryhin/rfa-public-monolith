package media.toloka.rfa.account.model;
/// Бухгалтерска операція (transaction), що містить проводки (posting)

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import media.toloka.rfa.account.model.base.AccBaseTransaction;
import media.toloka.rfa.radio.model.Clientdetail;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
//@Table
//        (indexes = {
//        @Index(columnList = "uuid"),
//        @Index(columnList = "id")}
//)
public class AccTransaction extends AccBaseTransaction {


    @Expose
    private String name;

    @Expose
    private String comment;

    @Expose
    private Date date = new Date();

    @Expose
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "transaction", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
//    @OneToMany( fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
    private List<AccPosting> operationList = new ArrayList<>();

    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "operator")
    private Clientdetail operator;

}
