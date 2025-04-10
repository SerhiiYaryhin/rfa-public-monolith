package media.toloka.rfa.radio.stt.repositore;

import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.newstoradio.model.News;
import media.toloka.rfa.radio.stt.model.Stt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface SttRepositore extends JpaRepository<Stt, Long>, PagingAndSortingRepository<Stt, Long> {

    List<Stt> findByClientdetail(Clientdetail cd);

    Page findByClientdetailOrderByCreatedateDesc(Pageable storePage, Clientdetail cd);

    Page findAllByOrderByCreatedateDesc(Pageable storePage);

    List<Stt> findByClientdetailOrderByCreatedateDesc(Clientdetail cd);

    Stt getByUuid(String newsUuid);

//    List<News> findByCategoryOrderByCreatedateDesc(ENewsCategory eNewsCategory);

}
