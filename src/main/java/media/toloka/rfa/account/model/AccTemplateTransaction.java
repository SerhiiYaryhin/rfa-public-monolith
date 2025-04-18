package media.toloka.rfa.account.model;
///  Типова операція (transaction) з типовими проводками (posting)
import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import media.toloka.rfa.account.model.base.AccBaseTransaction;

import java.util.List;

@Data
@Entity
public class AccTemplateTransaction extends AccBaseTransaction {
//    @Id
//    @Expose
//    private String uuid;
//    @Expose
//    private Long id;
    @Expose
    private String name;
    @Expose
    private String comment;
    @Expose
    @OneToMany(mappedBy = "transaction", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AccTemplatePosting> entry;

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
