package media.toloka.rfa.account.model.Documents;
// замовлення на виконання роботи

import com.google.gson.annotations.Expose;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;
import media.toloka.rfa.account.model.polymorphing.AccBaseEntityDoc;
import media.toloka.rfa.account.model.polymorphing.iface.PolymorphicTarget;

import java.util.Date;

@Data
@Entity
public class AccOrderedWorkDocument  extends AccBaseEntityDoc implements PolymorphicTarget  {

    @Override
    public String getTypeCode() {
        return "ORDERWORK";
    }
}
