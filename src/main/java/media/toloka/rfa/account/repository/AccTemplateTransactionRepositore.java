package media.toloka.rfa.account.repository;

import media.toloka.rfa.account.model.AccTemplateTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccTemplateTransactionRepositore extends
        PagingAndSortingRepository<AccTemplateTransaction, String>,
        JpaRepository<AccTemplateTransaction, String> {

    AccTemplateTransaction getByUuid(String uuid);
}
