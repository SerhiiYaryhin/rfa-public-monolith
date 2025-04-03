package media.toloka.rfa.radio.repository;

//import media.toloka.rfa.radio.client.model.Clientdetail;
import media.toloka.rfa.security.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserRepository extends JpaRepository<Users, Long> {
//    Long saveUser(Users user);

//    Optional<Users> getByEmail(String email);
//    Users getUserByEmail(String email);
//    Clientdetail getByUser(Users user);
//    void save(Users user);

    Users getUserByEmail(String email);

    @Query(value = "SELECT a FROM Users a WHERE "  // a WHERE "
            + "LOWER(a.email) LIKE LOWER(CONCAT('%', ?1, '%'))")
    List<Users> findUsersByTemplateEmail(String Template);

    List<Users> findAllByOrderByIdAsc();

    List<Users> findAllByOrderByIdDesc();
}
