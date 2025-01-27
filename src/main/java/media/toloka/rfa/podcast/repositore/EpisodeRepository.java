package media.toloka.rfa.podcast.repositore;

import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.podcast.model.PodcastItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;


public interface EpisodeRepository
        extends JpaRepository<PodcastItem, Long>, PagingAndSortingRepository<PodcastItem, Long> {
//    List<PodcastItem> findByClientdetail(Clientdetail cd);
//    PodcastChannel getByStoreuuid(String storeUuid);
    PodcastItem getByUuid(String ItemUuid);
    PodcastItem save(PodcastItem episode);

    List<PodcastItem> findByClientdetailOrderByIdDesc(Clientdetail cd);
    List<PodcastItem> findByTitle(String title);

    PodcastItem getByTitle(String title);
}
