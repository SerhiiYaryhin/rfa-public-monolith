package media.toloka.rfa.radio.contract;
// куерування контрактами користувача
// створення, оплата, закриття, пауза, зняття з паузи.

import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Contract;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.radio.model.enumerate.EContractStatus;
//import media.toloka.rfa.radio.model.enumerate.EHistoryType;
import media.toloka.rfa.radio.contract.service.ContractService;
import media.toloka.rfa.radio.history.service.HistoryService;
import media.toloka.rfa.radio.station.service.StationService;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import media.toloka.rfa.radio.client.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static media.toloka.rfa.radio.model.enumerate.EContractStatus.CONTRACT_FREE;
import static media.toloka.rfa.radio.model.enumerate.EContractStatus.CONTRACT_PAY;
import static media.toloka.rfa.radio.model.enumerate.EHistoryType.History_UserCreateContract;
import static media.toloka.rfa.radio.model.enumerate.EHistoryType.History_StationChange;


@Slf4j
@Controller
public class ClientHomeContractController {

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ContractService contractService;

    @Autowired
    private StationService stationService;

    final Logger logger = LoggerFactory.getLogger(ClientHomeContractController.class);

    @GetMapping(value = "/user/contract")
    public String ContractHome(
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            logger.warn("User not found. Redirect to main page");
            return "redirect:/";
        }
        // дивимося його групи
        // відповідним чином виводимо пункти меню
        // Заповнюємо поля для форми
        List<Contract> contracts = contractService.ListContractByUser(user);
        //==========================================================
        // Бавимося
        //==========================================================
        model.addAttribute("contracts",  contracts);
        model.addAttribute("userID",    user.getId());
        model.addAttribute("userName",  user.getEmail());
        return "/user/contract";
    }

    @GetMapping(value = "/user/contractedit")
    public String getEditContract(
            @RequestParam(value = "id", required = true) Long id,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            logger.warn("User not found. Redirect to main page");
            return "redirect:/";
        }
        // Редагуємо контракт
        // передали id контракту
        Contract contract = contractService.GetContractById(id);
        if (contract == null) {
            logger.info("Контракт з id={} не знайдено", id.toString());
            // TODO Вивести в форму повідомлення, що контракт не знайдено
            return "redirect:/user/contract";
        }
        List<Station> stationContractList = stationService.GetListStationByClientAndContract(user.getClientdetail(), contract);
        List<Station> stationWoContract = stationService.GetListStationByClientAndContract(user.getClientdetail(), null);


        List<EContractStatus> options = new ArrayList<EContractStatus>();
        options.add(CONTRACT_FREE);
        options.add(CONTRACT_PAY);
        model.addAttribute("options", options);

        model.addAttribute("stationContractList",  stationContractList);
        model.addAttribute("stationWoContract",  stationWoContract);
        model.addAttribute("contract",  contract);
//        model.addAttribute("info", "======= message ======");
        return "/user/contractedit";
    }

    @GetMapping(value = "/user/delstationfromcontract")
    public String postDelStationFromContract (
            @RequestParam(value = "id", required = true) Long id,
            @RequestParam(value = "cnt", required = true) Long contract_id,
            Model model
    ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            logger.warn("User not found. Redirect to main page");
            return "redirect:/";
        }
        Contract contract = contractService.GetContractById(contract_id);
        Station station = stationService.GetStationById(id);
        station.setContract(null);
        stationService.saveStation(station);
        historyService.saveHistory(History_StationChange,
                " Видалили станцію "+ station.getUuid()
                + " з контракту " + contract.getUuid(),
                user);

        List<Station> stationContractList = stationService.GetListStationByClientAndContract(user.getClientdetail(), contract);
        List<Station> stationWoContract = stationService.GetListStationByClientAndContract(user.getClientdetail(), null);


        List<EContractStatus> options = new ArrayList<EContractStatus>();
        options.add(CONTRACT_FREE);
        options.add(CONTRACT_PAY);
        model.addAttribute("options", options);

        model.addAttribute("stationContractList",  stationContractList);
        model.addAttribute("stationWoContract",  stationWoContract);
        model.addAttribute("contract",  contract);

        return "/user/contractedit";
    }



    @GetMapping(value = "/user/addstationtocontract")
    public String postAddStationToContract (
            @RequestParam(value = "id", required = true) Long id,
            @RequestParam(value = "cnt", required = true) Long contract_id,
            Model model
    ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            logger.warn("User not found. Redirect to main page");
            return "redirect:/";
        }
        Contract contract = contractService.GetContractById(contract_id);
        Station station = stationService.GetStationById(id);
        if ((contract.getContractStatus() == CONTRACT_FREE) && (contract.getStationList().size() > 0)) {
            // Не можемо додати на безкоштовний контакт більше однієї станції.
            logger.info("Не можемо додати на безкоштовний контакт більше однієї станції. ");
            model.addAttribute("warning",  "Не можемо додати на контакт більше однієї тестової станції. ");
        } else {
            station.setContract(contract);
            stationService.saveStation(station);
            historyService.saveHistory(History_StationChange,
                    " Додали станцію "+ station.getUuid()
                            + " до контракту " + contract.getUuid(),
                    user);
        }

        List<EContractStatus> options = new ArrayList<EContractStatus>();
        options.add(CONTRACT_FREE);
        options.add(CONTRACT_PAY);
        model.addAttribute("options", options);

        List<Station> stationContractList = stationService.GetListStationByClientAndContract(user.getClientdetail(), contract);
        List<Station> stationWoContract = stationService.GetListStationByClientAndContract(user.getClientdetail(), null);

        model.addAttribute("stationContractList",  stationContractList);
        model.addAttribute("stationWoContract",  stationWoContract);
        model.addAttribute("contract",  contract);

        return "/user/contractedit";
    }


    @PostMapping(value = "/user/contractedit")
    public String postEditContract(
            @ModelAttribute ("contract") Contract fcontract,
//            @PathVariable Long id,
            Model model ) {
        // Редагуємо контракт
        // передали id контракту
//        Contract contract = contractService.GetContractById(id);
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            logger.warn("User not found. Redirect to main page");
            return "redirect:/";
        }
        Contract contract = contractService.GetContractById(fcontract.getId());
        if (contract == null) {
            logger.info("Контракт з id={} не знайдено", fcontract.getId().toString());
            // TODO Вивести в форму повідомлення, що контракт збережено
            return "redirect:/user/contract";
        }
        if (fcontract.getContractStatus().label.equals(CONTRACT_FREE.label)) {
            contract.setContractStatus(CONTRACT_FREE);
        } else {
            contract.setContractStatus(CONTRACT_PAY);
        }
        contract.setUsercomment(fcontract.getUsercomment());
        contract.setContractname(fcontract.getContractname());
        contractService.saveContract(contract);
        logger.info("Контракт з UUID={} збережено", contract.getUuid());
        return "redirect:/user/contract";
    }

    @GetMapping(value = "/user/createcontract")
    public String getuserCreateContract(
            @ModelAttribute Contract contract,
            Model model ) {
        // перевірити чи увійшли
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            logger.warn("User not found. Redirect to main page");
            return "redirect:/";
        }
        Contract ncontract = new Contract();
        ncontract.setContractStatus(CONTRACT_FREE);
        ncontract.setNumber( UUID.randomUUID().toString());
        ncontract.setClientdetail(clientService.GetClientDetailByUser(clientService.GetCurrentUser()));
        model.addAttribute("contract",  ncontract);
        return "/user/createcontract";
    }

    @PostMapping(value = "/user/createcontract")
    public String postuserCreateContract(
            @ModelAttribute Contract contract,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            logger.warn("User not found. Redirect to main page");
            return "redirect:/";
        }
        Clientdetail clientdetail = user.getClientdetail();
        // Працюємо з новим контрактом.
        Contract ncontract = new Contract();
//        ncontract.setContractStatus(CONTRACT_FREE);
//        ncontract.setNumber(UUID.randomUUID().toString());
//        ncontract.setUuid(ncontract.getNumber());
//        ncontract.setCreateDate(LocalDateTime.now());
        ncontract.setLastPayDate(null);
        ncontract.setClientdetail(clientService.GetClientDetailByUser(clientService.GetCurrentUser()));
        ncontract.setUsercomment(contract.getUsercomment());
        ncontract.setContractname(contract.getContractname());

        if (clientdetail.getContractList().size() > 0) {
            ncontract.setContractStatus(CONTRACT_PAY);
        }
        contractService.saveContract(ncontract);
        Clientdetail cl = clientService.GetClientDetailByUser(user);
//        cl.getContractList().add(ncontract);
        clientService.SaveClientDetail(cl);
        historyService.saveHistory(History_UserCreateContract, " Новий контракт: "+ncontract.getNumber().toString(), user);

        return "redirect:/user/contract";
    } // END postuserCreateContract
}
