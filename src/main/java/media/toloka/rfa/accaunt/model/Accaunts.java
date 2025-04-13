package media.toloka.rfa.accaunt.model;

import com.google.gson.annotations.Expose;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
public class Accaunts {
    @Id
    @Expose
    private String uuid;

    @Expose
    @GeneratedValue
    private Long id;

    @Expose
    private Long acc;
    private String accname;
    private String acccomment;

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
