package media.toloka.rfa.radio.model;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;


@Data
@Entity
@Table(indexes = @Index(columnList = "uuid"))
public class PostCategory {
    @Id
    @GeneratedValue
    @Expose
    private Long id;
    @Expose
    private String uuid= UUID.randomUUID().toString();
    @Expose
    private String label;
    @Expose
    private Boolean rootPage;
    @Expose
    @ManyToOne(cascade = {CascadeType.ALL})
    private PostCategory parent = null;

}
