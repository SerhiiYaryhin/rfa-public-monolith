package media.toloka.rfa.account.model.Documents;

import com.google.gson.annotations.Expose;
import jakarta.persistence.Entity;
import lombok.Data;
import media.toloka.rfa.account.model.polymorphing.AccBaseEntityDoc;
import media.toloka.rfa.account.model.polymorphing.iface.PolymorphicTarget;

// Акт Виконаних робіт
// формується на підставі окремих виконаних робіт
@Data
@Entity
public class AccActCompletedWorkDocument extends AccBaseEntityDoc implements PolymorphicTarget  {


    @Override
    public String getTypeCode() {
        return "ACTCOMPLETEWORK";
    }
}
