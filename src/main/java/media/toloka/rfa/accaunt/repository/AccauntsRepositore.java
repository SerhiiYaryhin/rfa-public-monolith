package media.toloka.rfa.accaunt.repository;

import media.toloka.rfa.accaunt.model.Accaunts;
import media.toloka.rfa.podcast.model.PodcastChannel;
import media.toloka.rfa.radio.store.model.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface AccauntsRepositore extends PagingAndSortingRepository<Accaunts, Long>, JpaRepository<Accaunts, Long> {

    /// Перелік в плані рахунків
    List<Accaunts> findall();
    Accaunts getByUuid(String uuid);

}
