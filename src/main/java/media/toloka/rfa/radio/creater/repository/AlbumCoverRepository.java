package media.toloka.rfa.radio.creater.repository;


import media.toloka.rfa.radio.model.Albumсover;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.store.model.EStoreFileType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlbumCoverRepository extends JpaRepository<Albumсover, Long> {

//    Optional
    List<Albumсover> findByClientdetail(Clientdetail cd);
    //List<Albumсover> findByClientdetailAnd(Clientdetail cd, EStoreFileType sft);
    Albumсover getReferenceById(Long id);
}
