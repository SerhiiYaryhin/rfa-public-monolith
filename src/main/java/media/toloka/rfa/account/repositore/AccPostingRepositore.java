package media.toloka.rfa.account.repositore;

import media.toloka.rfa.account.model.AccPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AccPostingRepositore extends
        PagingAndSortingRepository<AccPosting, UUID>,
        JpaRepository<AccPosting, UUID> {

}
