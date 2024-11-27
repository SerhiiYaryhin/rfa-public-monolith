package media.toloka.rfa.radio.post.repositore;

import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Post;
import media.toloka.rfa.radio.model.enumerate.EPostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface PostRepositore extends JpaRepository<Post, Long>, PagingAndSortingRepository<Post, Long> {
    List<Post> findByClientdetail(Clientdetail cd);
    Post getById(Long id);
    List<Post> findByApruveOrderByCreatedateDesc(Boolean apruve);

    Page findAllByOrderByPublishdateDesc(Pageable storePage);

    List<Post> findByClientdetailOrderByCreatedateDesc(Clientdetail cd);

    Post getByUuid(String postUuid);

    List<Post> getByCategory(EPostCategory postCategory);
}
