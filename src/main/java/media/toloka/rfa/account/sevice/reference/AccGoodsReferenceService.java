package media.toloka.rfa.account.sevice.reference;


import media.toloka.rfa.account.model.referens.AccGoodsReference;
import media.toloka.rfa.account.repositore.referens.AccGoodsReferenceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccGoodsReferenceService {

    private final AccGoodsReferenceRepository repository;

    public AccGoodsReferenceService(AccGoodsReferenceRepository repository) {
        this.repository = repository;
    }

    public List<AccGoodsReference> findAll() {
        return repository.findAll();
    }

    public Optional<AccGoodsReference> findByUiid(UUID id) {
        return repository.findById(id);
    }

    public AccGoodsReference save(AccGoodsReference goods) {
        return repository.save(goods);
    }

    public void deleteById(UUID uuid) {
        repository.deleteById(uuid);
    }
}
