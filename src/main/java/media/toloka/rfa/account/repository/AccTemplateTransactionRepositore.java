package media.toloka.rfa.account.repository;

import media.toloka.rfa.account.model.AccTemplateTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AccTemplateTransactionRepositore extends
        PagingAndSortingRepository<AccTemplateTransaction, UUID>,
        JpaRepository<AccTemplateTransaction, UUID> {

    AccTemplateTransaction getByUuid(UUID uuid);
}
