package media.toloka.rfa.account.repositore.transaction;

import media.toloka.rfa.account.model.transaction.AccTemplatePosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AccTemplatePostingRepositore extends
        PagingAndSortingRepository<AccTemplatePosting, UUID>,
        JpaRepository<AccTemplatePosting, UUID> {

}
