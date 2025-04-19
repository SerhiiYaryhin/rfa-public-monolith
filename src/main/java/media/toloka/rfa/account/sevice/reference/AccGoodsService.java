package media.toloka.rfa.account.sevice.reference;


import media.toloka.rfa.account.model.referens.AccGoodsReference;
import media.toloka.rfa.account.repositore.referens.AccGoodsReferenceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccGoodsService {

    private final AccGoodsReferenceRepository repository;

    public AccGoodsService(AccGoodsReferenceRepository repository) {
        this.repository = repository;
    }

    public List<AccGoodsReference> FindAll() {
        return repository.findAll();
    }

    public Optional<AccGoodsReference> FindByUiid(UUID id) {
        return repository.findById(id);
    }

    public AccGoodsReference Save(AccGoodsReference goods) {
        return repository.save(goods);
    }

    public void DeleteById(UUID uuid) {
        repository.deleteById(uuid);
    }
}
