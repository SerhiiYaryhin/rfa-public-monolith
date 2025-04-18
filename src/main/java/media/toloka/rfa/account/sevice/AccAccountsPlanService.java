package media.toloka.rfa.account.sevice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.account.model.AccAccountsPlan;
import media.toloka.rfa.account.repositore.AccAccountsPlanRepositore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccAccountsPlanService {

    @Autowired
    AccAccountsPlanRepositore accAccountsPlanRepositore;

    public AccAccountsPlan GetByUuid(UUID uuid) {
        return accAccountsPlanRepositore.getByUuid(uuid);
    }

    public Page FindAll(PageRequest acc) {
        return accAccountsPlanRepositore.findAll(acc);
    }

    public AccAccountsPlan Save(AccAccountsPlan acc) {
        return accAccountsPlanRepositore.save(acc);
    }

    public AccAccountsPlan GetByAcc(Long numberAcc) {
        return accAccountsPlanRepositore.getByAcc(numberAcc);
    }

    public void Delete(AccAccountsPlan acc) {
        accAccountsPlanRepositore.delete(acc);
    }
}
