package media.toloka.rfa.account.repository;

import media.toloka.rfa.account.model.AccPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface AccOperationRepositore extends
        PagingAndSortingRepository<AccPosting, String>, JpaRepository<AccPosting, String>
{

    /// Перелік в плані рахунків
    List<AccPosting> findAll();
    AccPosting getByUuid(String uuid);

}
