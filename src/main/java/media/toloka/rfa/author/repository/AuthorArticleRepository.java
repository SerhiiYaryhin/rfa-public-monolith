package media.toloka.rfa.author.repository;

import media.toloka.rfa.author.model.AuthorArticle;
import media.toloka.rfa.author.model.AuthorColumn;
import media.toloka.rfa.author.model.enumerate.EAuthorArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthorArticleRepository extends JpaRepository<AuthorArticle, Long> {
    Optional<AuthorArticle> findByUuid(String uuid);
    Optional<AuthorArticle> findBySlug(String slug);
    Page<AuthorArticle> findByColumnAndStatus(AuthorColumn column, EAuthorArticleStatus status, Pageable pageable);
    Page<AuthorArticle> findByStatus(EAuthorArticleStatus status, Pageable pageable);
}
