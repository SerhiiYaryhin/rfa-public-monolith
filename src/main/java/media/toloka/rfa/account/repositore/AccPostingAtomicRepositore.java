package media.toloka.rfa.account.repositore;

import media.toloka.rfa.account.model.AccPostingAtomic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AccPostingAtomicRepositore extends
        PagingAndSortingRepository<AccPostingAtomic, UUID>,
        JpaRepository<AccPostingAtomic, UUID> {

}
