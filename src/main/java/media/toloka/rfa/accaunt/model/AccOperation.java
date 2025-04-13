package media.toloka.rfa.accaunt.model;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import media.toloka.rfa.radio.model.Clientdetail;

import java.util.UUID;

@Data
@Entity
public class AccOperation {
    @Id
    @Expose
    private String uuid;

    @Expose
    @GeneratedValue
    private Long id;

    @Expose
    private EAccJobType jobtype;
    @Expose
    private Long sum;

    @Expose
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "debitacc_uuid")
    private AccAccaunts debitacc;

    @Expose
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "creditacc_uuid")
    private AccAccaunts creditacc;

    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "clientdetail_id")
    private Clientdetail client;
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "operator_id")
    private Clientdetail operator;

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
