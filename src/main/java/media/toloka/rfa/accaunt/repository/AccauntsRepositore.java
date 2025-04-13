package media.toloka.rfa.accaunt.repository;

import media.toloka.rfa.accaunt.model.AccAccaunts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface AccauntsRepositore extends
        PagingAndSortingRepository<AccAccaunts, String>,
        JpaRepository<AccAccaunts, String> {

    /// Перелік в плані рахунків
    List<AccAccaunts> findAll();
    AccAccaunts getByUuid(String uuid);
    AccAccaunts save(AccAccaunts acc);

}
