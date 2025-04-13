package media.toloka.rfa.accaunt.service;

import media.toloka.rfa.accaunt.model.Accaunts;
import media.toloka.rfa.accaunt.repository.AccauntsRepositore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccService {

    @Autowired
    private AccauntsRepositore accRepositore;

    /// Перелік в плані рахунків
    public List<Accaunts> GetListAccaunts() {
        return accRepositore.findall();
    }

    public Accaunts GetAccauntByUUID(String uuid) {
        return accRepositore.getByUuid(uuid);
    }
}
