package media.toloka.rfa.account.model;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(indexes = {
        @Index(columnList = "uuid"),
        @Index(columnList = "id")}
)
public class AccOperationSet {
    @Id
    @Expose
    private String uuid;

    @Expose
    @GeneratedValue
    private Long id;


    @Expose
    @ToString.Exclude
//    @OneToMany(mappedBy = "acc_operation", fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
    @OneToMany( fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
    private List<AccOperation> operationList = new ArrayList<>();


    @Expose
    private String comment;

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
