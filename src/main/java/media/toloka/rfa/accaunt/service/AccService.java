package media.toloka.rfa.accaunt.service;

import media.toloka.rfa.accaunt.model.AccAccaunts;
import media.toloka.rfa.accaunt.repository.AccauntsRepositore;
import media.toloka.rfa.radio.model.Clientdetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccService {

    @Autowired
    private AccauntsRepositore accRepositore;

    /// Перелік в плані рахунків
    public List<AccAccaunts> GetListAccaunts() {
        return null; //accRepositore.findAll();
    }

    public AccAccaunts GetAccAccauntByUUID(String uuid) {
        return accRepositore.getByUuid(uuid);
    }

    public Page GetPage(int pageNumber, int pageCount) {
        return accRepositore.findAll(PageRequest.of(pageNumber, pageCount));
    }

}
