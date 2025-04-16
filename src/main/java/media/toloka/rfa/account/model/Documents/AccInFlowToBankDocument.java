package media.toloka.rfa.account.model.Documents;
// надходження грошей на рахунок

import com.google.gson.annotations.Expose;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;
import media.toloka.rfa.account.model.polymorphing.iface.PolymorphicTarget;

//@Data
//@Entity
public class AccInFlowToBankDocument { //implements PolymorphicTarget {
//    @Id
//    @Expose
//    private String uuid;
//    @Expose
//    @GeneratedValue
//    private Long id;
//    @Expose
//    private Long docNumber; // Номер документа

//    @Override
//    public String getTypeCode() {
//        return "FLOWTOBANK";
//    }
}
