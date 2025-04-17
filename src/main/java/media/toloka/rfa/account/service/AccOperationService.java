package media.toloka.rfa.account.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.account.model.AccTemplateTransaction;
import media.toloka.rfa.account.repository.AccCachFlowRepositore;
import media.toloka.rfa.account.repository.AccRepositore;
import media.toloka.rfa.account.repository.AccTemplateTransactionRepositore;
import media.toloka.rfa.radio.model.Clientdetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccOperationService {
    @Autowired
    private AccRepositore accRepositore;

//    @Autowired
//    private AccCachFlowRepositore accCachFlowRepositore;

    @Autowired
    private final AccTemplateTransactionRepositore transactionRepository;

    /// проводка документу
    public void RunOperation(AccTemplateTransaction accTT, BigDecimal value, Clientdetail cd, Clientdetail operatorCD, Boolean mode) {
//        for (AccTemplateEntry accTE : accTT.getEntry()) {
//            AccCashFlow accCF = new AccCashFlow();
//            accCF.setAcc();
//        }
    }
}
