package media.toloka.rfa.account.model;

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
//    @OneToMany(fetch = FetchType.EAGER)
//    @OneToMany(mappedBy = "transaction", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<AccTemplateEntry> entry;
    @OneToMany(mappedBy = "transaction", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AccTemplateEntry> entry;

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
