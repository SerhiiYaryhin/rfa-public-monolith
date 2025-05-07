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

    Page findAllByOrderByPublishdateDesc(Pageable storePage);

    Page findAllByPublishdateIsNotNullOrderByPublishdateDesc(Pageable storePage);

    List<Post> findByClientdetailOrderByCreatedateDesc(Clientdetail cd);

    Post getByUuid(String postUuid);

    List<Post> getByCategoryOrderByPublishdateDesc(EPostCategory postCategory);

    List<Post> getByPostcategoryOrderByPublishdateDesc(PostCategory postCategory);
}
