package media.toloka.rfa.account.repository;

import media.toloka.rfa.account.model.AccCashFlow;
import media.toloka.rfa.account.model.AccPostingAtomic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccPostingAtomicRepositore extends
        PagingAndSortingRepository<AccPostingAtomic, String>,
        JpaRepository<AccPostingAtomic, String> {

}
