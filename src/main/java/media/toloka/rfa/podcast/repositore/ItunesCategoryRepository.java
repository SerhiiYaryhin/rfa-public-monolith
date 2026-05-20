package media.toloka.rfa.podcast.repositore;

import media.toloka.rfa.podcast.model.PodcastChannel;
import media.toloka.rfa.podcast.model.PodcastItunesCategory;
import media.toloka.rfa.radio.model.Clientdetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface ItunesCategoryRepository
        extends JpaRepository<PodcastItunesCategory, String>, PagingAndSortingRepository<PodcastItunesCategory, String> {

    List<PodcastItunesCategory> findByChanel(ChanelRepository icr);
    //    PodcastChannel getByStoreuuid(String storeUuid);
    PodcastItunesCategory getByUuid(String ChanelUuid);
    PodcastItunesCategory save(PodcastItunesCategory icr);


}
