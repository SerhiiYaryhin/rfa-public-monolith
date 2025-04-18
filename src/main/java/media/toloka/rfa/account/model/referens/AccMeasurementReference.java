package media.toloka.rfa.account.model.referens;
/// Довідник одиниць виміру
import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import media.toloka.rfa.account.model.base.AccBaseDoc;

@EqualsAndHashCode(callSuper = false)
@Data
@Entity
public class AccMeasurementReference extends AccBaseDoc {

    @Expose
    private String name;
    @Expose
    @Column(columnDefinition = "TEXT")
    private String comments;
}
