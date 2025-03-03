package media.toloka.rfa.radio.newstoradio.repository;

import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Post;
import media.toloka.rfa.radio.newstoradio.model.ENewsCategory;
import media.toloka.rfa.radio.newstoradio.model.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface NewsRepositore extends JpaRepository<News, Long>, PagingAndSortingRepository<News, Long> {
    List<Post> findByClientdetail(Clientdetail cd);
    Page findByClientdetailOrderByCreatedateDesc(Pageable storePage, Clientdetail cd);

    Page findAllByOrderByCreatedateDesc(Pageable storePage);

    List<News> findByClientdetailOrderByCreatedateDesc(Clientdetail cd);

    News getByUuid(String newsUuid);

    List<News> findByCategoryOrderByCreatedateDesc(ENewsCategory eNewsCategory);

}
