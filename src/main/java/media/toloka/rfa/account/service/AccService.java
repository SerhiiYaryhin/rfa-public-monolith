package media.toloka.rfa.account.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.account.model.AccAccountsPlan;
import media.toloka.rfa.account.model.AccTemplatePosting;
import media.toloka.rfa.account.model.AccTemplateTransaction;
import media.toloka.rfa.account.model.polymorphing.AccBaseEntityDoc;
import media.toloka.rfa.account.repository.AccCachFlowRepositore;
import media.toloka.rfa.account.repository.AccRepositore;
import media.toloka.rfa.account.repository.AccTemplateEntryRepositore;
import media.toloka.rfa.account.repository.AccTemplateTransactionRepositore;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccService {

    @Autowired
    private ClientService clientService;

    @Autowired
    private AccRepositore accRepositore;

    @Autowired
    private AccCachFlowRepositore accCachFlowRepositore;

    @Autowired
    private final AccTemplateTransactionRepositore accTemplateTransactionRepositore;

    @Autowired
    private final AccTemplateEntryRepositore entryRepository;
    /// Перелік в плані рахунків
    public List<AccAccountsPlan> GetListAccounts() {
        return null; //accRepositore.findAll();
    }

    public AccAccountsPlan GetAccAccountByUUID(UUID uuid) {
        return accRepositore.getByUuid(uuid);
    }

    public Page GetPageAcc(int pageNumber, int pageCount) {
        return accRepositore.findAll(PageRequest.of(pageNumber, pageCount, Sort.by("acc").ascending()));
    }

    public AccAccountsPlan Save(AccAccountsPlan acc) {
        if (acc.getUuid() == null) acc.setUuid(UUID.randomUUID());
        if (acc.getId() == null) acc.setId(System.currentTimeMillis()); // Метод для генерації унікального ID
        if (acc.getDocCreate() == null) acc.setDocCreate(new Date());
        acc.setOperator( clientService.GetClientDetailByUser(clientService.GetCurrentUser()));
        return accRepositore.save(acc);
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
//        if (transaction.getUuid() == null) transaction.generateUUID();
        AccTemplateTransaction curTrancaction = GetTemplareTransaction(transaction.getUuid());
        if (curTrancaction == null) {
            log.info("Зберігаємо нову");
        }
        for (AccTemplatePosting entry : transaction.getEntry()) {
            if (entry.getUuid() == null) entry.generateUUID();
            entry.setTransaction(transaction);
        }
        accTemplateTransactionRepositore.save(transaction);
        entryRepository.saveAll(transaction.getEntry());
    }


    public List<AccTemplateTransaction> findAllTransaction() {
        return accTemplateTransactionRepositore.findAll();
    }

    public AccAccountsPlan GetAccByNumder(Long number) {
        return accRepositore.getByAcc(number);
    }

    public void DelAcc(AccAccountsPlan acc) {
        accRepositore.delete(acc);
    }

    public AccTemplateTransaction GetTemplareTransaction(String uuid) {
       return accTemplateTransactionRepositore.getByUuid(UUID.fromString(uuid));
    }
}
