package media.toloka.rfa.radio.model;
// повідомлення з головної сторінки. Можна надсилати без реєстрації.

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;
import java.util.UUID;


@Data
@Entity
@Table(name="rootsitemessage")
public class MessageFromSite {

// приймаємо повідомлення з форми на сайті
// додати можливість завантаження декількох файлів

    @Id
    @GeneratedValue
    private Long contact_id;
    @Expose
    private String uuid= UUID.randomUUID().toString();
    private String name;
    private String email;
    private String phone;
    @Column(columnDefinition = "TEXT")
    private String message;
    private Date contact_datetime;

}