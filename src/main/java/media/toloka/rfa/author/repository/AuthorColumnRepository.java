package media.toloka.rfa.author.repository;

import media.toloka.rfa.author.model.AuthorColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthorColumnRepository extends JpaRepository<AuthorColumn, Long> {
    Optional<AuthorColumn> findByUuid(String uuid);
    Optional<AuthorColumn> findBySlug(String slug);
    Optional<AuthorColumn> findByAuthorUuid(String authorUuid);
}
