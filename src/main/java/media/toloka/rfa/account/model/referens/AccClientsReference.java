package media.toloka.rfa.account.model.referens;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;
import media.toloka.rfa.account.model.base.AccBaseReference;
import media.toloka.rfa.radio.model.Clientdetail;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
public class AccClientsReference extends AccBaseReference {
    @OneToOne
    private Clientdetail cd;
}
