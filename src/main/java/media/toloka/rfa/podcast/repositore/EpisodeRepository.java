package media.toloka.rfa.podcast.repositore;

import media.toloka.rfa.podcast.model.PodcastChannel;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.podcast.model.PodcastItem;
import media.toloka.rfa.radio.store.model.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;


public interface EpisodeRepository
        extends JpaRepository<PodcastItem, Long>, PagingAndSortingRepository<PodcastItem, Long> {
//    List<PodcastItem> findByClientdetail(Clientdetail cd);
//    PodcastChannel getByStoreuuid(String storeUuid);
    PodcastItem getByUuid(String episodeUuid);
    List<PodcastItem> findByEnclosurestore(Store store);
    List<PodcastItem> findByImagestoreitem(Store store);
    PodcastItem save(PodcastItem episode);

    Page<PodcastItem> findByChanelOrderByPubDateDesc(PodcastChannel chanel, Pageable pageable);

    List<PodcastItem> findByClientdetailOrderByIdDesc(Clientdetail cd);
    List<PodcastItem> findByClientdetailOrderByIdDesc(String cduuid);

    List<PodcastItem> findByTitle(String title);

    PodcastItem getByTitle(String title);
}
