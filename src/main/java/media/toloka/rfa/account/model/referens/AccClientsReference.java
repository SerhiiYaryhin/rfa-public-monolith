package media.toloka.rfa.account.model.referens;

import jakarta.persistence.OneToOne;
import media.toloka.rfa.account.model.base.AccBaseReference;
import media.toloka.rfa.radio.model.Clientdetail;

public class AccClientsReference extends AccBaseReference {
    @OneToOne
    private Clientdetail client;
}
