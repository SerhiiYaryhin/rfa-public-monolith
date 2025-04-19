package media.toloka.rfa.account.sevice.reference;


import media.toloka.rfa.account.model.referens.AccClientsReference;
import media.toloka.rfa.account.model.referens.AccGoodsReference;
import media.toloka.rfa.account.repositore.referens.AccClientsReferenceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccClientsService {

    private final AccClientsReferenceRepository repository;

    public AccClientsService(AccClientsReferenceRepository repository) {
        this.repository = repository;
    }

    public List<AccClientsReference> FindAll() {
        return repository.findAll();
    }

    public Optional<AccClientsReference> FindByUuid(UUID id) {
        return repository.findById(id);
    }

    public AccClientsReference Save(AccClientsReference goods) {
        return repository.save(goods);
    }

    public void DeleteById(UUID uuid) {
        repository.deleteById(uuid);
    }
}
