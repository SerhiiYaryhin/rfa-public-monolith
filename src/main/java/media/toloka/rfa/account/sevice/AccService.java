package media.toloka.rfa.account.sevice;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.account.model.accplan.AccAccountsPlan;
import media.toloka.rfa.account.model.transaction.AccTemplateTransaction;
import media.toloka.rfa.account.repositore.transaction.AccTemplateTransactionRepositore;
import media.toloka.rfa.radio.client.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccService {

    @Autowired
    private ClientService clientService;

    @Autowired
    private AccAccountsPlanService accAccountsPlanService;

    @Autowired
    private final AccTemplateTransactionRepositore accTemplateTransactionRepositore;

        /// Перелік в плані рахунків
    public List<AccAccountsPlan> GetListAccounts() {
        return null; //accRepositore.findAll();
    }

    public AccAccountsPlan GetAccAccountByUUID(UUID uuid) {
        return accAccountsPlanService.GetByUuid(uuid);
    }

    public Page GetPageAcc(int pageNumber, int pageCount) {
        return accAccountsPlanService.FindAll(PageRequest.of(pageNumber, pageCount, Sort.by("acc").ascending()));
    }

    public AccAccountsPlan Save(AccAccountsPlan acc) {
        if (acc.getUuid() == null) acc.setUuid(UUID.randomUUID());
//        if (acc.getId() == null) acc.setId(System.currentTimeMillis()); // Метод для генерації унікального ID
//        if (acc.getDocCreate() == null) acc.setDocCreate(new Date());
        acc.setOperator( clientService.GetClientDetailByUser(clientService.GetCurrentUser()));
        return accAccountsPlanService.Save(acc);
    }

//    public Page GetPageCachFlowAll(int pageNumber, int pageCount) {
//        Page accSummaryDtoList = accCachFlowRepositore.getAccsWithTotalValues();
//        return accRepositore.findAll(PageRequest.of(pageNumber, pageCount));
//    }

//    public List<AccSummaryDto> GetCachFlowAll() {
//        List<AccSummaryDto> accSummaryDtoList = accCachFlowRepositore.getAccsWithTotalValues();
//        return accSummaryDtoList;
//    }

//    public List<AccSummaryDto> getAccsWithSumOnMaxDate() {
//        List<AccSummaryDto> accsWithSumOnMaxDate = accCachFlowRepositore.getAccsWithSumOnMaxDate();
//        return accsWithSumOnMaxDate;
//    }

    // transaction Service

    @Transactional
    public void SaveTransaction(AccTemplateTransaction transaction) {
//        AccTemplateTransaction curTrancaction = GetTemplareTransaction(transaction.getUuid());
//        if (curTrancaction == null) {
//            log.info("Зберігаємо нову");
//        }
//        for (AccTemplatePosting entry : transaction.getEntry()) {
//            if (entry.getUuid() == null) entry.generateUUID();
//            entry.setTransaction(transaction);
//        }
//        accTemplateTransactionRepositore.save(transaction);
//        entryRepository.saveAll(transaction.getEntry());
    }


    public List<AccTemplateTransaction> findAllTransaction() {
        return accTemplateTransactionRepositore.findAll();
    }

    public AccAccountsPlan GetAccByNumder(Long number) {
        return accAccountsPlanService.GetByAcc(number);
    }

    public void DelAcc(AccAccountsPlan acc) {
        accAccountsPlanService.Delete(acc);
    }

    public AccTemplateTransaction GetTemplareTransaction(String uuid) {
       return accTemplateTransactionRepositore.getByUuid(UUID.fromString(uuid));
    }
}
