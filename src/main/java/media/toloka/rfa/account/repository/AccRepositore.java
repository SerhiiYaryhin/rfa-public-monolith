package media.toloka.rfa.account.repository;

import media.toloka.rfa.account.model.AccAccounts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface AccRepositore extends
        PagingAndSortingRepository<AccAccounts, String>,
        JpaRepository<AccAccounts, String> {

    /// Перелік в плані рахунків
    List<AccAccounts> findAll();
    AccAccounts getByUuid(String uuid);
    AccAccounts save(AccAccounts acc);
    AccAccounts getByAcc(Long acc);

}
