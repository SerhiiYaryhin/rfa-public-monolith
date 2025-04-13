package media.toloka.rfa.accaunt.repository;

import media.toloka.rfa.accaunt.model.AccCashFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface AccCachFlowRepositore extends
        PagingAndSortingRepository<AccCashFlow, String>,
        JpaRepository<AccCashFlow, String>
{

    /// Перелік в плані рахунків
    List<AccCashFlow> findAll();
    AccCashFlow getByUuid(String uuid);

}
