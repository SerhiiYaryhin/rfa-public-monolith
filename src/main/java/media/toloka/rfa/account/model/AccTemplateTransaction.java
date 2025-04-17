package media.toloka.rfa.account.model;
///  Типова операція (transaction) з типовими проводками (posting)
import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Entity
public class AccTemplateTransaction {
    @Id
    @Expose
    private String uuid;
    @Expose
    private Long id;
    @Expose
    private String name;
    @Expose
    private String comment;
    @Expose
    @OneToMany(mappedBy = "transaction", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AccTemplatePosting> entry;

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
