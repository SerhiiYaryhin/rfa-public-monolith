package media.toloka.rfa.author.repository;

import media.toloka.rfa.author.model.AuthorRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthorRequestRepository extends JpaRepository<AuthorRequest, Long> {
    Optional<AuthorRequest> findByUuid(String uuid);
}
