package media.toloka.rfa.radio.repository;

import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.security.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClientDetailRepository extends JpaRepository<Clientdetail, Long> {
    List<Clientdetail> getByUser(Users user);

    Clientdetail getByUuid(String clientUUID);

    @Query(value = "SELECT a FROM Clientdetail a WHERE "
            + "LOWER(a.custname) LIKE LOWER(CONCAT('%', ?1, '%')) OR "
            + "LOWER(a.custsurname) LIKE LOWER(CONCAT('%', ?1, '%')) OR "
            + "LOWER(a.uuid) LIKE LOWER(CONCAT('%', ?1, '%')) OR "
            + "LOWER(a.firmname) LIKE LOWER(CONCAT('%', ?1, '%'))")
    List<Clientdetail> findByTemplate(String template);

    Clientdetail getUserByTelegramuser(String telegramUser);

    List<Clientdetail> findByProfilephoto(media.toloka.rfa.radio.store.model.Store profilephoto);
}
