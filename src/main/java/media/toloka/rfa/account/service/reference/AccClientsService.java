package media.toloka.rfa.account.service.reference;


import media.toloka.rfa.account.model.reference.AccClientsReference;
import media.toloka.rfa.account.model.reference.AccGoodsReference;
import media.toloka.rfa.account.repository.reference.AccClientsReferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
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
