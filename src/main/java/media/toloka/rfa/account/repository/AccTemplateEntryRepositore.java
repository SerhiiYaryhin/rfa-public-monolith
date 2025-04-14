package media.toloka.rfa.account.repository;

import media.toloka.rfa.account.model.AccTemplateEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccTemplateEntryRepositore extends
        PagingAndSortingRepository<AccTemplateEntry, String>,
        JpaRepository<AccTemplateEntry, String> {

}
