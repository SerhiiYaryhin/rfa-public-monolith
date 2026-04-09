package media.toloka.rfa.account.service.reference;


import media.toloka.rfa.account.model.reference.AccGoodsReference;
import media.toloka.rfa.account.repository.reference.AccGoodsReferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AccGoodsService {

    private final AccGoodsReferenceRepository repository;

    public AccGoodsService(AccGoodsReferenceRepository repository) {
        this.repository = repository;
    }

    public List<AccGoodsReference> FindAll() {
        return repository.findAll();
    }

    public Optional<AccGoodsReference> FindByUuid(UUID id) {
        return repository.findById(id);
    }

    public AccGoodsReference Save(AccGoodsReference goods) {
        return repository.save(goods);
    }

    public void DeleteById(UUID uuid) {
        repository.deleteById(uuid);
    }
}
