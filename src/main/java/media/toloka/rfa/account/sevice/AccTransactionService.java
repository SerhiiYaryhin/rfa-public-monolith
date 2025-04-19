package media.toloka.rfa.account.sevice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.account.repositore.transaction.AccPostingAtomicRepositore;
import media.toloka.rfa.account.repositore.transaction.AccPostingRepositore;
import media.toloka.rfa.account.repositore.transaction.AccTransactionRepositore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccTransactionService {
    @Autowired
    private AccAccountsPlanService accAccountsPlanService;
    @Autowired
    private AccTransactionRepositore accTransactionRepositore;
    @Autowired
    private AccPostingRepositore accPostingRepositore;
    @Autowired
    private AccPostingAtomicRepositore accPostingAtomicRepositore;


}

