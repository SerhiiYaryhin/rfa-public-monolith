package media.toloka.rfa.radio.repository;

//import media.toloka.rfa.model.Clientdetail;
//import media.toloka.rfa.model.Us;

import media.toloka.rfa.media.messanger.model.ChatMessage;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.security.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClientDetailRepository extends JpaRepository<Clientdetail, Long> {
//    Clientdetail getClientdetailByUser(Users user);
//    Optional<Clientdetail> findClientdetailByUser(Users user);
//    Clientdetail getByUser(Users user);
    List<Clientdetail> getByUser(Users user);

    Clientdetail getByUuid(String clientUUID);

//    @Query("SELECT cm FROM ChatMessage cm WHERE cm.roomuuid IS NULL and (cm.fromuuid = ?1 or cm.touuid = ?1) ORDER BY send ASC LIMIT 1000")
//    List<ChatMessage> findByPrivateMessageByUserUuid(String useruuid);

    @Query(value = "SELECT a FROM Clientdetail a WHERE "  // a WHERE "
//            + "LOWER(a.custname) LIKE LOWER(CONCAT('%', ?1, '%')) OR "
            + "LOWER(a.custname) LIKE LOWER(CONCAT('%', ?1, '%')) OR "
            + "LOWER(a.custsurname) LIKE LOWER(CONCAT('%', ?1, '%')) OR "
            + "LOWER(a.uuid) LIKE LOWER(CONCAT('%', ?1, '%')) OR "
            + "LOWER(a.firmname) LIKE LOWER(CONCAT('%', ?1, '%'))") // OR " +
    List<Clientdetail> findByTemplate(String template);

    Clientdetail getUserByTelegramuser(String telegramUser);
//    List<Clientdetail> findByTemplate(@Param("template")  String template);
//            + "LOWER(a.firmname) LIKE LOWER(CONCAT('%', :template, '%'))") // OR " +
//            + "LOWER(a.custname) LIKE LOWER(CONCAT('%', :template, '%')) OR "
//            + "LOWER(a.custsurname) LIKE LOWER(CONCAT('%', :template, '%')) OR "
//            "LOWER(a.category) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
//            "LOWER(a.keywords) LIKE LOWER(CONCAT('%', :searchText, '%'))")


//    @Query("SELECT a FROM Article a WHERE " +
//            "LOWER(a.content) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
//            "LOWER(a.title) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
//            "LOWER(a.author) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
//            "LOWER(a.category) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
//            "LOWER(a.keywords) LIKE LOWER(CONCAT('%', :searchText, '%'))")
}
