package media.toloka.rfa.account.repository;

import media.toloka.rfa.account.model.AccOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface AccOperationRepositore extends
        PagingAndSortingRepository<AccOperation, String>, JpaRepository<AccOperation, String>
{

    /// Перелік в плані рахунків
    List<AccOperation> findAll();
    AccOperation getByUuid(String uuid);

}
