package media.toloka.rfa.accaunt.model;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
@Table(indexes = {
        @Index(columnList = "uuid"),
        @Index(columnList = "id")}
)
public class AccAccaunts {
    @Id
    @Expose
    private String uuid = UUID.randomUUID().toString();

    @Expose
    @GeneratedValue
    private Long id;

    @Expose
    private Long acc;
    @Expose
    private String accname;
    @Expose
    private String operationcomment;

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
