package media.toloka.rfa.radio.store.Reposirore;

import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.store.model.EStoreFileType;
import media.toloka.rfa.radio.store.model.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

//public interface StoreRepositore extends JpaRepository<Store, Long> {
public interface StoreRepositorePagination extends PagingAndSortingRepository<Store, Long>,
        JpaRepository<Store, Long>
{
    List<Store> findAllByClientdetail(Clientdetail clientdetail);
    Store getByFilenameAndClientdetail(String filename, Clientdetail cd);
//    List<Store> findByClientdetailByStorefiletypeOrederByCreatedateByAsc(Clientdetail clientdetail, EStoreFileType storefiletype);
    Page<Store> findByStorefiletype(Pageable pageable, EStoreFileType storefiletype);

    Page findByClientdetail(Pageable storePage, Clientdetail cd);
    Page findByClientdetailOrderByIdDesc(Pageable storePage, Clientdetail cd);

    Store getByUuid(String uuid);
    Store getByFilepatch(String filepatch);

    List<Store> findByClientdetailAndStorefiletype(Clientdetail cd, EStoreFileType type);

    List<Store> findByClientdetailAndStorefiletypeOrderByIdDesc(Clientdetail cd, EStoreFileType type);

    @Query(value = "SELECT a FROM Store a WHERE "
            + "a.clientdetail = ?1 AND  "
            + "LOWER(a.contentMimeType) LIKE LOWER(CONCAT('%', 'image/', '%'))"
            + "ORDER BY a.createdate DESC") // OR " +
    Page findByStorelAllPictures(Pageable pageable, Clientdetail cd);

//    Page findByClientdetailOrderByIdDesc(Pageable storePage, Clientdetail cd);
}
