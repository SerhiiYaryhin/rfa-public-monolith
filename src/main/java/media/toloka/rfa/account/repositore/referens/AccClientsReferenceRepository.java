package media.toloka.rfa.account.repositore.referens;

import media.toloka.rfa.account.model.referens.AccClientsReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.UUID;

public interface AccClientsReferenceRepository extends
        PagingAndSortingRepository<AccClientsReference, UUID>,
        JpaRepository<AccClientsReference, UUID> {

}