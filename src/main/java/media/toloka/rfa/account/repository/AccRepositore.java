package media.toloka.rfa.account.repository;

import media.toloka.rfa.account.model.AccAccountsPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface AccRepositore extends
        PagingAndSortingRepository<AccAccountsPlan, String>,
        JpaRepository<AccAccountsPlan, String> {

    /// Перелік в плані рахунків
    List<AccAccountsPlan> findAll();
    AccAccountsPlan getByUuid(String uuid);
    AccAccountsPlan save(AccAccountsPlan acc);
    AccAccountsPlan getByAcc(Long acc);
    List<AccAccountsPlan> findAllByOrderByAccAsc();

}
