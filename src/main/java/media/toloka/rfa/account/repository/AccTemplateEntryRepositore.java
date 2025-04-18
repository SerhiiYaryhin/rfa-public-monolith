package media.toloka.rfa.account.repository;

import media.toloka.rfa.account.model.AccTemplatePosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AccTemplateEntryRepositore extends
        PagingAndSortingRepository<AccTemplatePosting, UUID>,
        JpaRepository<AccTemplatePosting, UUID> {

}
