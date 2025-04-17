package media.toloka.rfa.account.repository;

import media.toloka.rfa.account.model.AccPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccPostingRepositore extends
        PagingAndSortingRepository<AccPosting, String>,
        JpaRepository<AccPosting, String> {

}
