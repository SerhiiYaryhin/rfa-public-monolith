package media.toloka.rfa.radio.station;

import lombok.Data;
import media.toloka.rfa.media.messanger.model.MessageRoom;
import media.toloka.rfa.media.messanger.model.enumerate.EChatRoomType;
import media.toloka.rfa.media.messanger.service.MessangerService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.contract.service.ContractService;
import media.toloka.rfa.radio.history.service.HistoryService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Contract;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.radio.model.enumerate.EContractStatus;
import media.toloka.rfa.radio.station.dto.StationCredentialsDTO;
import media.toloka.rfa.radio.station.service.StationService;
import media.toloka.rfa.rsa_toradio.SendToRadio;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.Date;

import static media.toloka.rfa.radio.model.enumerate.EHistoryType.History_StationChange;
import static media.toloka.rfa.rpc.model.ERPCJobType.*;

@Profile("Front")
@Controller
public class ClientHomeStationController {

    private final Logger logger = LoggerFactory.getLogger(ClientHomeStationController.class);

    @Autowired
    private ClientService clientService;

    @Autowired
    private ContractService contractService;

    @Autowired
    private StationService stationService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private MessangerService messangerService;

    @Autowired
    private media.toloka.rfa.radio.document.service.DocumentService documentService;

    @Autowired
    private SendToRadio sendToRadio;

    @GetMapping(value = "/user/toradiouser")
    public String getUserToradioUser(@RequestParam Long id, Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        Station station = stationService.GetStationById(id);
        if (station == null) {
            model.addAttribute("warning", "Не вдалося знайти станцію.");
            return "/user/stations";
        }

        StationCredentialsDTO creds = new StationCredentialsDTO(station.getId(), station.getToradiouser(), "");
        model.addAttribute("toradiouser", creds);
        model.addAttribute("station", station);
        return "/user/settoradiouser";
    }

    @PostMapping(value = "/user/toradiousersave")
    public String postUserToradioUser(@ModelAttribute StationCredentialsDTO creds) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        Station station = stationService.GetStationById(creds.getId());
        if (station != null && (station.getToradiouser() == null || station.getToradiouser().equals(creds.getUsername()))) {
            station.setToradiouser(creds.getUsername());
            station.setToradiopassword(sendToRadio.encrypt(creds.getPassword()));
            stationService.saveStation(station);
        }
        return "redirect:/user/controlstation?id=" + station.getId();
    }

    @GetMapping(value = "/user/stationcreateroom/{suuid}")
    public String createStationChatRoom(@PathVariable String suuid) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        Station station = stationService.GetStationByUUID(suuid);
        if (station != null && station.getRoomuuid() == null) {
            MessageRoom stRoom = new MessageRoom();
            stRoom.setRoomname(station.getName());
            stRoom.setRoomtype(EChatRoomType.CHATROOM_TYPE_STATION);
            messangerService.SaveRoom(stRoom);
            station.setRoomuuid(stRoom.getUuid());
            stationService.saveStation(station);
        }
        return "redirect:/user/controlstation?id=" + (station != null ? station.getId() : "");
    }

    @GetMapping(value = "/user/stationslivebroadcast/{suuid}")
    public String toggleLiveBroadcast(@PathVariable String suuid) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        Station station = stationService.GetStationByUUID(suuid);
        if (station != null) {
            MessageRoom room = messangerService.GetChatRoomByUUID(station.getRoomuuid());
            room.setRoomOnlineStatus(!room.getRoomOnlineStatus());
            room.setStartonline(room.getRoomOnlineStatus() ? new Date() : null);
            messangerService.SaveRoom(room);
            return "redirect:/user/controlstation?id=" + station.getId();
        }
        return "redirect:/user/stations";
    }

    @GetMapping(value = "/user/stations")
    public String listUserStations(Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";
        model.addAttribute("stations", stationService.GetListStationByUser(user));
        return "/user/stations";
    }

    @GetMapping(value = "/user/station/wizard")
    public String stationWizard(Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        Clientdetail cd = clientService.GetClientDetailByUser(user);
        model.addAttribute("cd", cd);
        model.addAttribute("addresses", clientService.GetClientAddressList(cd));
        model.addAttribute("documents", documentService.listDocumentsByClientdetail(cd));
        return "/user/wizard";
    }

    @GetMapping(value = "/user/createstation")
    public String createStation(Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";
        
        Clientdetail cd = clientService.GetClientDetailByUser(user);
        String error = stationService.validateStationCreation(cd);
        
        if (error != null) {
            model.addAttribute("warning", error);
            return "/user/stations";
        }

        Station station = stationService.CreateStation(cd);
        if (station == null) {
            model.addAttribute("warning", "Помилка при створенні станції. Зверніться до підтримки.");
            return "/user/stations";
        }

        stationService.triggerJob(station, user, 
            JOB_STATION_CREATE, JOB_STATION_ALLOCATE, JOB_STATION_PREPARE_NGINX, 
            JOB_STATION_LIBRETIME_MIGRATE, JOB_STATION_START, JOB_STATION_STOP);

        return "redirect:/user/stations";
    }

    @GetMapping(value = "/user/controlstation")
    public String controlStation(@RequestParam Long id, Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        Station station = stationService.GetStationById(id);
        if (station == null) return "redirect:/user/stations";

        model.addAttribute("formUserPSW", new Users());
        model.addAttribute("contracts", contractService.ListContractByUser(user));
        model.addAttribute("linkstation", stationService.GetURLStation(station));
        model.addAttribute("station", station);
        model.addAttribute("stationstateonline", stationService.GetStationRoomStatus(station.getRoomuuid()));
        
        MessageRoom room = messangerService.GetChatRoomByUUID(station.getRoomuuid());
        model.addAttribute("roomstart", room != null ? room.getStartonline() : null);

        return "/user/controlstation";
    }

    @PostMapping(value = "/user/stationsave")
    public String saveStationSettings(@ModelAttribute Station settings, @ModelAttribute Users formUserPSW, Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        Station station = stationService.GetStationById(settings.getId());
        if (station == null) return "redirect:/user/stations";

        station.setName(settings.getName());
        station.setIcecastdescription(settings.getIcecastdescription());
        station.setIcecastname(settings.getIcecastname());
        station.setIcecastgenre(settings.getIcecastgenre());
        station.setIcecastsite(settings.getIcecastsite());
        station.setLastchangedate(new Date());
        stationService.saveStation(station);

        historyService.saveHistory(History_StationChange, "Збережено налаштування станції " + station.getUuid(), user);

        if (formUserPSW.getPassword() != null && !formUserPSW.getPassword().isBlank()) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(formUserPSW.getPassword().getBytes());
                user.setPassword(new String(Hex.encode(md.digest())));
                stationService.triggerJob(station, user, JOB_STATION_SETPASSWORD);
            } catch (Exception e) {
                logger.error("MD5 hash error: {}", e.getMessage());
            }
        }

        return "redirect:/user/stations";
    }

    @GetMapping(value = "/user/startstation")
    public String startStation(@RequestParam Long id, Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        Station station = stationService.GetStationById(id);
        if (station == null) return "redirect:/user/stations";

        Contract contract = station.getContract();
        if (contract == null || (contract.getClientdetail().getAccount() < 0 && contract.getContractStatus() == EContractStatus.CONTRACT_PAY)) {
            model.addAttribute("warning", "Недостатньо коштів або відсутній контракт.");
            return "redirect:/user/stations";
        }

        stationService.triggerJob(station, user, JOB_STATION_START);
        return "redirect:/user/stations";
    }

    @GetMapping(value = "/user/stopstation")
    public String stopStation(@RequestParam Long id) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        Station station = stationService.GetStationById(id);
        if (station != null) stationService.triggerJob(station, user, JOB_STATION_STOP);
        return "redirect:/user/stations";
    }

    @GetMapping(value = "/user/deletestation")
    public String deleteStation(@RequestParam Long id) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        Station station = stationService.GetStationById(id);
        if (station != null) {
            stationService.triggerJob(station, user, JOB_STATION_DELETE);
            historyService.saveHistory(History_StationChange, "Запит на видалення станції " + station.getUuid(), user);
        }
        return "redirect:/user/stations";
    }

    @GetMapping(value = "/user/psstation")
    public String checkStationProcess(@RequestParam Long id) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        Station station = stationService.GetStationById(id);
        if (station != null) stationService.triggerJob(station, user, JOB_STATION_GET_PS);
        return "redirect:/user/stations";
    }
}
