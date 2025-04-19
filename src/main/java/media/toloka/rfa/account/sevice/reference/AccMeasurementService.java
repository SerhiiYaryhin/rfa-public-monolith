package media.toloka.rfa.account.sevice.reference;


import media.toloka.rfa.account.model.referens.AccMeasurementReference;
import media.toloka.rfa.account.repositore.referens.AccMeasurementReferenceRepositore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccMeasurementService {

    private final AccMeasurementReferenceRepositore repository;

    public AccMeasurementService(AccMeasurementReferenceRepositore repository) {
        this.repository = repository;
    }

    public List<AccMeasurementReference> FindAll() {
        return repository.findAll();
    }

    public Optional<AccMeasurementReference> FindByUiid(UUID id) {
        return repository.findById(id);
    }

    public AccMeasurementReference Save(AccMeasurementReference goods) {
        return repository.save(goods);
    }

    public void DeleteById(UUID uuid) {
        repository.deleteById(uuid);
    }
}
