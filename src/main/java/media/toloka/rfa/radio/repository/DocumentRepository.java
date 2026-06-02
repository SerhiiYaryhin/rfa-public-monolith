package media.toloka.rfa.radio.repository;




import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Documents;
import media.toloka.rfa.radio.model.enumerate.EDocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface DocumentRepository extends JpaRepository<Documents, Long> {

    List<Documents> findByClientdetail(Clientdetail Clientdetailrfa);

    Documents getById(Long id);

    List<Documents> findByStatusLessThan(EDocumentStatus eDocumentStatus);
    List<Documents> findByStatusNot(EDocumentStatus eDocumentStatus);

    List<Documents> findByStore(media.toloka.rfa.radio.store.model.Store store);
    List<Documents> findByStoreuuid(String storeuuid);

    Documents getByUuid(String uuid);
}
