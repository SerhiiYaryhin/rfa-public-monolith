package media.toloka.rfa.account.repository;

import media.toloka.rfa.account.model.AccCashFlow;
import media.toloka.rfa.account.model.dto.AccSummaryDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.UUID;

public interface AccCachFlowRepositore extends
        PagingAndSortingRepository<AccCashFlow, UUID>,
        JpaRepository<AccCashFlow, UUID>
{

    /// Перелік в плані рахунків
    List<AccCashFlow> findAll();
    AccCashFlow getByUuid(UUID uuid);

//    @Query("""
//    SELECT new media.toloka.rfa.account.model.dto.AccSummaryDto(
//        a.uuid,
//        a.id,
//        a.acc,
//        a.accname,
//        a.operationcomment,
//        SUM(f.value)
//    )
//    FROM AccCashFlow f
//    JOIN f.acc a
//    GROUP BY a.uuid, a.id, a.acc, a.accname, a.operationcomment
//    """)
//    List<AccSummaryDto> getAccsWithTotalValues();

//    @Query("""
//    SELECT new media.toloka.rfa.account.model.dto.AccSummaryDto(
//        a.uuid,
//        a.id,
//        a.acc,
//        a.accname,
//        a.operationcomment,
//        SUM(f.value)
//    )
//    FROM AccCashFlow f
//    JOIN f.acc a
//    WHERE f.operationdate = (
//        SELECT MAX(f2.operationdate) FROM AccCashFlow f2
//    )
//    GROUP BY a.uuid, a.id, a.acc, a.accname, a.operationcomment
//    """)
//    List<AccSummaryDto> getAccsWithSumOnMaxDate();

}
