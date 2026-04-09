package media.toloka.rfa.account.repository.reference;

import media.toloka.rfa.account.model.accplan.AccAccountsPlan;
import media.toloka.rfa.account.model.reference.AccGoodsReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.UUID;

public interface AccGoodsReferenceRepository extends
        PagingAndSortingRepository<AccGoodsReference, UUID>,
        JpaRepository<AccGoodsReference, UUID> {

}