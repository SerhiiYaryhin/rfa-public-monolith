package media.toloka.rfa.account.model;
/// Бухгалтерска операція (transaction), що містить проводки (posting)

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import media.toloka.rfa.account.model.base.AccBaseEntityDoc;
import media.toloka.rfa.radio.model.Clientdetail;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
//@Table
//        (indexes = {
//        @Index(columnList = "uuid"),
//        @Index(columnList = "id")}
//)
public class AccTransaction extends AccBaseEntityDoc {


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

    @Expose
    private String docType;
    @Expose
    private Long id;
    @PrePersist
    public void setDefaults() {
        this.docType = getTypeCode();
        if (this.id == null) this.id = System.currentTimeMillis(); // Метод для генерації унікального ID
    }
    @Override
    public String getTypeCode() {
        String className = this.getClass().getSimpleName(); ;
        return className;
    }
}
