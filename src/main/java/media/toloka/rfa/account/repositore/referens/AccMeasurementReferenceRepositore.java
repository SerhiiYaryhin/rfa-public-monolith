package media.toloka.rfa.account.repositore.referens;

import media.toloka.rfa.account.model.accplan.AccAccountsPlan;
import media.toloka.rfa.account.model.referens.AccGoodsReference;
import media.toloka.rfa.account.model.referens.AccMeasurementReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.UUID;

public interface AccMeasurementReferenceRepositore extends
        PagingAndSortingRepository<AccMeasurementReference, UUID>,
        JpaRepository<AccMeasurementReference, UUID> {

}