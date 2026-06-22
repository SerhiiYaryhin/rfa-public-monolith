package media.toloka.rfa.radio.station.service;

import com.google.gson.Gson;
import media.toloka.rfa.config.gson.service.GsonService;
import media.toloka.rfa.media.messanger.service.MessangerService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.contract.service.ContractService;
import media.toloka.rfa.radio.history.service.HistoryService;
import media.toloka.rfa.radio.model.Clientaddress;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Contract;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.radio.repository.StationRepo;
import media.toloka.rfa.rpc.model.RPCJob;
import media.toloka.rfa.rpc.model.ERPCJobType;
import media.toloka.rfa.security.model.Users;
import media.toloka.rfa.service.RfaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static media.toloka.rfa.radio.model.enumerate.EContractStatus.CONTRACT_FREE;
import static media.toloka.rfa.radio.model.enumerate.EContractStatus.CONTRACT_PAY;
import static media.toloka.rfa.radio.model.enumerate.EHistoryType.History_StationCreate;
import static media.toloka.rfa.rpc.model.ERPCJobType.*;

@Service
@Transactional
public class StationService {

    private final Logger logger = LoggerFactory.getLogger(StationService.class);

    @Autowired
    private StationRepo stationRepo;

    @Autowired
    private MessangerService messangerService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ContractService contractService;

    @Autowired
    private RfaService rfaService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private GsonService gsonService;

    @Value("${rabbitmq.queue}")
    private String queueNameRabbitMQ;

    @Value("${media.toloka.rfa.server.libretime.guiserver}")
    private String guiserver;

    @Value("${media.toloka.rfa.station.basename}")
    private String basestationurl;

    public List<Station> listAll() {
        return stationRepo.findAll();
    }

    public void saveStation(Station station) {
        stationRepo.save(station);
    }

    public Station GetStationById(long id) {
        return stationRepo.findById(id).orElse(null);
    }

    public void delete(long id) {
        stationRepo.deleteById(id);
    }

    public List<Station> GetListStationByUser(Users user) {
        Clientdetail cl = clientService.GetClientDetailByUser(user);
        return stationRepo.findStationByClientdetail(cl);
    }

    /**
     * Валідація можливості створення станції.
     * @return Текст помилки або null, якщо валідація успішна.
     */
    public String validateStationCreation(Clientdetail cd) {
        if (!cd.getConfirminfo()) {
            return "Ви не надали згоду з правилами користування сервісом. У Профайлі відмітьте поле \"З умовами надання сервісу погоджуюся\".";
        }
        
        List<Clientaddress> addresses = clientService.GetClientAddressList(cd);
        if (addresses.isEmpty()) {
            return "У Профайлі відсутня поштова адреса.";
        }

        boolean hasApproved = addresses.stream().anyMatch(Clientaddress::getApruve);
        if (!hasApproved) {
            return "У Профайлі відсутня схвалена адреса. Дочекайтеся будь ласка схвалення адміністрацією сервісу.";
        }

        if (!cd.getStationList().isEmpty()) {
            if (!HavePayContract(cd)) {
                if (CreateCheckFreeStation(cd)) {
                    return "У Вас вже є тестова станція що додана до безкоштовного контракту.";
                }
                return "У Вас немає комерційного контракту і вже є тестова станція.";
            }
        }
        return null;
    }

    /**
     * Створюємо станцію для клієнта
     *
     * @param clientdetail
     * @return
     */
    public Station CreateStation(Clientdetail clientdetail) {
        Station station = new Station();
        station.setName(null);
        SetStationDBName(station);
        station.setUuid(UUID.randomUUID().toString());
        station.setGuiserver(guiserver);
        station.setCreatedate(new Date());
        station.setClientdetail(clientdetail);
        
        clientdetail.getStationList().add(station);
        clientService.SaveClientDetail(clientdetail);
        
        historyService.saveHistory(History_StationCreate, "Нова станція: " + station.getUuid(), clientdetail.getUser());
        logger.info("Створено нову станцію в БД: {}", station.getUuid());
        return station;
    }

    /**
     * Встановлюємо імʼя для бази для LibreTime
     * @param st
     */
    public void SetStationDBName(Station st) {
        while (true) {
            String rstring = rfaService.GetRandomString(16);
            if (GetStationDBName(rstring) == null) {
                st.setDbname(rstring);
                return;
            }
        }
    }

    public Station GetStationDBName(String rstring) {
        return stationRepo.getStationByDbname(rstring);
    }

    /**
     * Отримуємо URL головної сторінки станції
     *
     * @param station
     * @return
     */
    public String GetURLStation(Station station) {
        return "https://" + station.getDbname() + "." + basestationurl;
    }

    public boolean HavePayContract(Clientdetail clientdetail) {
        List<Contract> contracts = contractService.FindContractByClientDetail(clientdetail);
        for (Contract c : contracts) {
            if (c.getContractStatus() == CONTRACT_PAY) return true;
        }
        return clientdetail.getStationList().isEmpty();
    }

    public boolean CreateCheckFreeStation(Clientdetail clientdetail) {
        List<Contract> contracts = contractService.FindContractByClientDetail(clientdetail);
        for (Contract c : contracts) {
            if (c.getContractStatus() == CONTRACT_FREE) {
                return !c.getStationList().isEmpty();
            }
        }
        return false;
    }

    public void triggerJob(Station station, Users user, ERPCJobType... types) {
        RPCJob rjob = new RPCJob();
        for (ERPCJobType type : types) {
            rjob.getJobchain().add(type);
        }
        rjob.setUser(user);

        Gson gson = gsonService.CreateGson();
        try {
            rjob.setRjobdata(gson.toJson(station));
            String payload = gson.toJson(rjob);
            rabbitTemplate.convertAndSend(queueNameRabbitMQ, payload);
            logger.info("Відправлено RPC завдання {} для станції {}", Arrays.toString(types), station.getUuid());
        } catch (Exception e) {
            logger.error("Помилка при відправці RPC завдання: {}", e.getMessage(), e);
        }
    }

    public void SetStationRunState(Station station, Boolean b) {
        Station st = stationRepo.getReferenceById(station.getId());
        st.setStationstate(b);
        stationRepo.save(st);
    }

    public List<Station> GetListStationByStatus(boolean b) {
        return stationRepo.getStationByStationstate(b);
    }

    public Station GetStationByUUID(String uuid) {
        return stationRepo.getStationByUuid(uuid);
    }

    public Boolean GetStationRoomStatus(String roomuuid) {
        if (roomuuid == null) return false;
        return messangerService.GetChatRoomByUUID(roomuuid).getRoomOnlineStatus();
    }

    public List<Station> GetListStationByClientAndContract(Clientdetail clientdetail, Contract contract) {
        return stationRepo.findStationByClientdetailAndContract(clientdetail, contract);
    }

    public List<Station> GetListStationByCd(Clientdetail cd) {
        return stationRepo.findStationByClientdetail(cd);
    }

    // Deprecated methods kept for backward compatibility if needed, but cleaned up
    public boolean CreateCheckConfirminfo(Clientdetail cd) { return cd.getConfirminfo(); }
    public boolean CreateCheckAddress(Clientdetail cd) { return !clientService.GetClientAddressList(cd).isEmpty(); }
    public boolean CreateCheckApruveAddress(Clientdetail cd) {
        return clientService.GetClientAddressList(cd).stream().anyMatch(Clientaddress::getApruve);
    }
}
