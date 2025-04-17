package media.toloka.rfa.account.model.Documents;
// виконана робота
// конкретна робота, що була виконана. Наприклад - текст -> голос, робота радіостанції тощо

import jakarta.persistence.Entity;
import lombok.Data;
import media.toloka.rfa.account.model.polymorphing.AccBaseEntityDoc;
import media.toloka.rfa.account.model.polymorphing.iface.PolymorphicTarget;

@Data
@Entity
public class AccCompletedWorkDocument extends AccBaseEntityDoc implements PolymorphicTarget {

    @Override
    public String getTypeCode() {
        return "COMPLETEWORK";
    }
}
