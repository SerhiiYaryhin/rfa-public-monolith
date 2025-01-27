package media.toloka.rfa.podcast.repositore;

import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.podcast.model.PodcastChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;


public interface ChanelRepository
        extends JpaRepository<PodcastChannel, Long>, PagingAndSortingRepository<PodcastChannel, Long> {
    List<PodcastChannel> findByClientdetail(Clientdetail cd);
//    PodcastChannel getByStoreuuid(String storeUuid);
    PodcastChannel getByUuid(String ChanelUuid);
    PodcastChannel save(PodcastChannel chanel);

    List<PodcastChannel> findByApruve(boolean b);
    List<PodcastChannel> findByPublishing(boolean b);
    List<PodcastChannel> findByTitle(String title);

}
