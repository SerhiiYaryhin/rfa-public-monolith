package media.toloka.rfa.radio.model;
// Використовується при реєстрації на порталі.
// при реєстрації створюється унікальний токен, який надсилається поштою користувачу

import jakarta.persistence.*;
import lombok.Data;
import media.toloka.rfa.security.model.Users;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

@Data
@Entity
//@Table(name="token")
public class Token {

    private static final int EXPIRATION = 60 * 24;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column
    private Long id;
    @Column
    private String token;
    @OneToOne(targetEntity = Users.class, fetch = FetchType.EAGER)
//    @JoinColumn(nullable = false, name = "id")
    private Users user;  // посилання на користувача, який намагається зареєструватися.
    @Column
    private Date expiryDate = calculateExpiryDate(EXPIRATION); // дата завершення дії токена. Поки не використовується.
    public Date calculateExpiryDate(int expiryTimeInMinutes) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Timestamp(cal.getTime().getTime()));
        cal.add(Calendar.MINUTE, expiryTimeInMinutes);
//        cal.add(Calendar.HOUR, expiryTimeInMinutes);
        return new Date(cal.getTime().getTime());
    }
}