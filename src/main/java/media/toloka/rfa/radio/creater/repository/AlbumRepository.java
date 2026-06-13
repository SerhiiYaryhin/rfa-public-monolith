package media.toloka.rfa.radio.creater.repository;


import media.toloka.rfa.radio.model.Album;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Track;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlbumRepository extends JpaRepository<Album, Long> {

//    Optional
    List<Album> findByClientdetail(Clientdetail cd);
    List<Album> findByStoreuuidalbumcover(String storeuuidalbumcover);
    Album getReferenceById(Long id);
}
