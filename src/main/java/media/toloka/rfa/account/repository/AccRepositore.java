package media.toloka.rfa.account.repository;

import media.toloka.rfa.account.model.AccAccountsPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.UUID;

public interface AccRepositore extends
        PagingAndSortingRepository<AccAccountsPlan, UUID>,
        JpaRepository<AccAccountsPlan, UUID> {

    /// Перелік в плані рахунків
    List<AccAccountsPlan> findAll();
    AccAccountsPlan getByUuid(UUID uuid);
    AccAccountsPlan save(AccAccountsPlan acc);
    AccAccountsPlan getByAcc(Long acc);
    List<AccAccountsPlan> findAllByOrderByAccAsc();

}
