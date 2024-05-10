package media.toloka.rfa.radio.contract.service;

import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Contract;
import media.toloka.rfa.radio.repository.ContractRepo;
import media.toloka.rfa.security.model.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContractService {

    @Autowired
    private ClientService clientService;

    @Autowired
    private ContractRepo contractRepo;
//    public List<Contract> listContractByUser(Users user) {
//        return contractRepo.findContractByUser(user);
//    }

    public void saveContract(Contract contract) {
        contractRepo.save(contract);
    }

    public List<Contract> ListContractByUser(Users user) {
        // TODO Коли працюємо як окремий сервіс, то clientService.GetCurrentUser() видасть null. Можуть бути проблеми
        // передбачити пошук по користувачу в базі ClientDetail
        Clientdetail cl = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        return contractRepo.findByClientdetail(cl);
    }

    public Contract GetContractById(Long id) {
        return contractRepo.getById(id);
    }

    public List<Contract> FindContractByClientDetail(Clientdetail clientDetail) {
        return contractRepo.findByClientdetail(clientDetail);
    }


}
