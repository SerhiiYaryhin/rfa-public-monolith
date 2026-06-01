package media.toloka.rfa.radio.post.repositore;

import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Post;
import media.toloka.rfa.radio.model.PostCategory;
import media.toloka.rfa.radio.model.enumerate.EPostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface PostRepositore extends JpaRepository<Post, Long>, PagingAndSortingRepository<Post, Long> {
    List<Post> findByClientdetail(Clientdetail cd);
    Page findByClientdetailOrderByCreatedateDesc(Pageable storePage, Clientdetail cd);


    Post getById(Long id);
    List<Post> findByApruveOrderByCreatedateDesc(Boolean apruve);

    List<Post> findAllByOrderByCreatedateDesc();

    Page findAllByOrderByPublishdateDesc(Pageable storePage);

    Page findAllByPublishdateIsNotNullOrderByPublishdateDesc(Pageable storePage);

    List<Post> findByClientdetailOrderByCreatedateDesc(Clientdetail cd);

    Post getByUuid(String postUuid);

    /** Отримати лише схвалені пости для публічного перегляду, виключаючи видалені та протерміновані */
    @org.springframework.data.jpa.repository.Query("SELECT p FROM Post p WHERE p.apruve = true AND p.postStatus != :status " +
            "AND (p.enddate IS NULL OR p.enddate > :now) " +
            "ORDER BY p.publishdate DESC")
    Page<Post> findPublicPosts(Pageable pageable, @org.springframework.data.repository.query.Param("status") media.toloka.rfa.radio.model.enumerate.EPostStatus status, @org.springframework.data.repository.query.Param("now") java.util.Date now);

    /** Отримати лише схвалені пости для публічного перегляду, виключаючи видалені */
    Page<Post> findByApruveTrueAndPostStatusNotOrderByPublishdateDesc(Pageable pageable, media.toloka.rfa.radio.model.enumerate.EPostStatus status);

    Page<Post> findByApruveTrueOrderByPublishdateDesc(Pageable pageable);

    List<Post> findByCoverstoreuuid(String coverstoreuuid);

    List<Post> getByCategoryOrderByPublishdateDesc(EPostCategory postCategory);

    List<Post> getByPostcategoryOrderByPublishdateDesc(PostCategory postCategory);
}
