package media.toloka.rfa.account.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import media.toloka.rfa.account.model.AccAccounts;
import media.toloka.rfa.account.model.AccTemplateEntry;
import media.toloka.rfa.account.model.AccTemplateTransaction;
import media.toloka.rfa.account.model.dto.AccSummaryDto;
import media.toloka.rfa.account.repository.AccCachFlowRepositore;
import media.toloka.rfa.account.repository.AccRepositore;
import media.toloka.rfa.account.repository.AccTemplateEntryRepositore;
import media.toloka.rfa.account.repository.AccTemplateTransactionRepositore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccService {

    @Autowired
    private AccRepositore accRepositore;

    @Autowired
    private AccCachFlowRepositore accCachFlowRepositore;

    @Autowired
    private final AccTemplateTransactionRepositore transactionRepository;

    @Autowired
    private final AccTemplateEntryRepositore entryRepository;
    /// Перелік в плані рахунків
    public List<AccAccounts> GetListAccounts() {
        return null; //accRepositore.findAll();
    }

    public AccAccounts GetAccAccountByUUID(String uuid) {
        return accRepositore.getByUuid(uuid);
    }

    public Page GetPageAcc(int pageNumber, int pageCount) {
        return accRepositore.findAll(PageRequest.of(pageNumber, pageCount));
    }

    public AccAccounts Save(AccAccounts acc) {
        return accRepositore.save(acc);
    }

//    public Page GetPageCachFlowAll(int pageNumber, int pageCount) {
//        Page accSummaryDtoList = accCachFlowRepositore.getAccsWithTotalValues();
//        return accRepositore.findAll(PageRequest.of(pageNumber, pageCount));
//    }

    public List<AccSummaryDto> GetCachFlowAll() {
        List<AccSummaryDto> accSummaryDtoList = accCachFlowRepositore.getAccsWithTotalValues();
        return accSummaryDtoList;
    }

    public List<AccSummaryDto> getAccsWithSumOnMaxDate() {
        List<AccSummaryDto> accsWithSumOnMaxDate = accCachFlowRepositore.getAccsWithSumOnMaxDate();
        return accsWithSumOnMaxDate;
    }

    // transaction Service

    @Transactional
    public void SaveTransaction(AccTemplateTransaction transaction) {
//        if (transaction.getUuid() == null) transaction.generateUUID();
        for (AccTemplateEntry entry : transaction.getEntry()) {
            if (entry.getUuid() == null) entry.generateUUID();
            entry.setTransaction(transaction);
        }
        transactionRepository.save(transaction);
        entryRepository.saveAll(transaction.getEntry());
    }


    public List<AccTemplateTransaction> findAllTransaction() {
        return transactionRepository.findAll();
    }

    public AccAccounts GetAccByNumder(Long number) {
        return accRepositore.getByAcc(number);
    }
}
