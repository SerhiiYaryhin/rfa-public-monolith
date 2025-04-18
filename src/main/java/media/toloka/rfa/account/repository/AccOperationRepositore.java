package media.toloka.rfa.account.repository;

import media.toloka.rfa.account.model.AccPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.UUID;

public interface AccOperationRepositore extends
        PagingAndSortingRepository<AccPosting, UUID>, JpaRepository<AccPosting, UUID>
{

    /// Перелік в плані рахунків
    List<AccPosting> findAll();
    AccPosting getByUuid(UUID uuid);

}
