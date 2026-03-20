package media.toloka.rfa.radio.creater.repository;


import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Track;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface TrackRepository extends JpaRepository<Track, Long>, PagingAndSortingRepository<Track, Long> {

//    Optional
    List<Track> findByClientdetail(Clientdetail cd);
    Page findByClientdetail(Pageable storePage, Clientdetail cd);
    Page findByClientdetailOrderByUploaddateDesc(Pageable storePage, Clientdetail cd);

    List<Track> findAllByOrderByUploaddateAsc();
    List<Track> findAllTop10ByOrderByUploaddateAsc();
//    List<Track> findTop10ByApruveTrueOrderByUploaddateAsc();
    @Query("SELECT t FROM Track t WHERE t.apruve = true ORDER BY t.uploaddate ASC LIMIT 10")
    List<Track> findTop10Approved();

    Track getById(Long id);
    Track getByStoreuuid(String storeUuid);

    Page findAllByOrderByUploaddateDesc(Pageable storePage);

    Track getByUuid(String trackUuid);


//    Page findAllOrderByUploaddateByAsc(Pageable storePage);
//
//    Page findAllOrderByUploaddateDesc(Pageable storePage);
//    findAllOrderByDateAsc
}
