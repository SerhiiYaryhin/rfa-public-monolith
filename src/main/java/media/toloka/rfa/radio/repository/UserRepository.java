package media.toloka.rfa.radio.repository;

//import media.toloka.rfa.radio.client.model.Clientdetail;
import media.toloka.rfa.security.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<Users, Long> {
//    Long saveUser(Users user);

//    Optional<Users> getByEmail(String email);
//    Users getUserByEmail(String email);
//    Clientdetail getByUser(Users user);
//    void save(Users user);

    Users getUserByEmail(String email);

    @Query("SELECT u FROM Users u LEFT JOIN u.clientdetail cd WHERE " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(cd.custname) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(cd.custsurname) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(cd.firmname) LIKE LOWER(CONCAT('%', :query, '%'))")
    org.springframework.data.domain.Page<Users> findUsersBySearchTemplate(@Param("query") String query, org.springframework.data.domain.Pageable pageable);

    @Query(value = "SELECT a FROM Users a WHERE "  // a WHERE "
            + "LOWER(a.email) LIKE LOWER(CONCAT('%', ?1, '%'))")
    List<Users> findUsersByTemplateEmail(String Template);

    List<Users> findAllByOrderByIdAsc();

    List<Users> findAllByOrderByIdDesc();

    @Query("SELECT DISTINCT u FROM Users u JOIN u.clientdetail cd JOIN cd.documentslist d WHERE d.status IN (media.toloka.rfa.radio.model.enumerate.EDocumentStatus.STATUS_LOADED, media.toloka.rfa.radio.model.enumerate.EDocumentStatus.STATUS_REVIEW)")
    org.springframework.data.domain.Page<Users> findUsersWithPendingDocuments(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT DISTINCT u FROM Users u JOIN u.clientdetail cd JOIN cd.clientaddressList a WHERE a.apruve = false")
    org.springframework.data.domain.Page<Users> findUsersWithPendingAddresses(org.springframework.data.domain.Pageable pageable);
}
