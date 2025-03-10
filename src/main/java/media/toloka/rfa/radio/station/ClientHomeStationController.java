package media.toloka.rfa.radio.station;


import com.google.gson.Gson;
import lombok.Data;
import media.toloka.rfa.config.gson.service.GsonService;
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
import media.toloka.rfa.radio.station.service.StationService;
import media.toloka.rfa.rpc.model.RPCJob;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.Date;
import org.springframework.security.crypto.codec.Hex;


import static media.toloka.rfa.radio.model.enumerate.EHistoryType.History_StationChange;
import static media.toloka.rfa.rpc.model.ERPCJobType.*;
@Profile("Front")
@Controller
public class ClientHomeStationController {

    @Data
    private class FormStr {
        private String name;
        private Long id;
    }

    @Value("${rabbitmq.queue}")
    private String queueNameRabbitMQ;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ContractService contractService;

    @Autowired
    private StationService stationService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private GsonService gsonService;

    @Autowired
    RabbitTemplate template;

    @Autowired
    private MessangerService messangerService;

    final Logger logger = LoggerFactory.getLogger(ClientHomeStationController.class);

    // Створюємо кімнату в чаті для радіостанції
    @GetMapping(value = "/user/stationcreateroom/{suuid}")
    public String userHomeStationCreateChatRoom(
            @PathVariable String suuid,
            Model model ) {

        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Station station = stationService.GetStationByUUID(suuid);
        if (station == null) {
            return "redirect:/";
        }
        if (station.getRoomuuid() == null) {
            MessageRoom stRoom = new MessageRoom();
            stRoom.setRoomname(station.getName());
            stRoom.setRoomtype(EChatRoomType.CHATROOM_TYPE_STATION);
            messangerService.SaveRoom(stRoom);
            station.setRoomuuid(stRoom.getUuid());
            stationService.saveStation(station);
        }
        return "redirect:/user/controlstation?id=" + station.getId().toString();
    }

    // Інвертуємо стан кімнати радіостанції OnLine/OffLine
    @GetMapping(value = "/user/stationslivebroadcast/{suuid}")
    public String userHomeStationLiveBroadcast(
            @PathVariable String suuid,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Station station = stationService.GetStationByUUID(suuid);
        if (station == null) {
            return "redirect:/";
        }
        MessageRoom msgroom = messangerService.GetChatRoomByUUID(station.getRoomuuid());
        msgroom.setRoomOnlineStatus(!msgroom.getRoomOnlineStatus());
        if (msgroom.getRoomOnlineStatus()) {
            msgroom.setStartonline(new Date());
        } else {
            msgroom.setStartonline(null);
        }
        messangerService.SaveRoom(msgroom);
//        model.addAttribute("stations",  stationService.GetListStationByUser(user));
        return "redirect:/user/controlstation?id="+station.getId().toString();
    }

    @GetMapping(value = "/user/stations")
    public String userHomeStation(
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        model.addAttribute("stations",  stationService.GetListStationByUser(user));
        return "/user/stations";
    }


    @GetMapping(value = "/user/createstation")
    public String userCreateStation(
            Model model ) {

//        logger.info("Create New station.");
        Users user = clientService.GetCurrentUser();
        Clientdetail clientdetail = clientService.GetClientDetailByUser(user);
        if (user == null) {
//            logger.warn("User not found. Redirect to main page");
            return "redirect:/";
        }
        if (stationService.CreateCheckConfirminfo(clientdetail) == false) {
            // TODO Відправити у форму повідомлення про неможливість створення станції та кинути клієнту месседж
            // TODO зробити запис в журнал
            // TODO Вісвітити повідомлення для користувача з причинами неможливості створити станцію
            model.addAttribute("warning", "Неможливо створити станцію! Ви не надали згоду з правилами користування сервісом. " +
                    "У Профайлі відмітте поле \"З умовами надання сервісу погоджуюся\".");
            return "/user/stations";
        }
        if (stationService.CreateCheckAddress(clientdetail) == false) {
            model.addAttribute("warning", "Неможливо створити станцію! У Профайлі відсутня поштова адреса.");
            return "/user/stations";
        }

        if (stationService.CreateCheckApruveAddress(clientdetail) == false) {
            model.addAttribute("warning", "Неможливо створити станцію! У Профайлі відсутня схвалена адреса. Дочекайтеся будь ласка схвалення адміністрацією сервісом.");
            return "/user/stations";
        }

        if (clientdetail.getStationList().size() > 0) {
            if (stationService.HavePayContract(clientdetail) == false) {
                // перевіряємо наявності безкоштовної станції
                if (stationService.CreateCheckFreeStation(clientdetail) == true) {
                    model.addAttribute("warning", "Неможливо створити станцію! У Вас вже є тестова станція що додана до безкоштовного контракту");
                    return "/user/stations";
                }
                model.addAttribute("warning", "Неможливо створити станцію! У Вас немає комерційного контракту і вже є тестова станція");
                return "/user/stations";
            }
        }

        Station station = stationService.CreateStation(clientdetail);
        if (station == null) {
            logger.info("Не можемо створити станцію для користувача {}. ", user.getEmail());
            model.addAttribute("warning", "Не можемо створити станцію. Повідомте про це службі підтримки.");
            return "/user/stations";
        }
//        historyService.saveHistory(History_StatiionCreate, " Нова станція: "
//                        +station.getUuid()
//                        +"для користувача " + clientdetail.getUser().getEmail(),
//                clientdetail.getUser());
        RPCJob rjob = new RPCJob();
        rjob.getJobchain().add(JOB_STATION_CREATE);
        rjob.getJobchain().add(JOB_STATION_ALLOCATE);
        rjob.getJobchain().add(JOB_STATION_PREPARE_NGINX);
        rjob.getJobchain().add(JOB_STATION_LIBRETIME_MIGRATE);
        rjob.getJobchain().add(JOB_STATION_START);
        rjob.getJobchain().add(JOB_STATION_STOP);
//        rjob.setRJobType(JOB_STATION_CREATE); // Створюємо необхідну інформацію в базі для станції
        rjob.setUser(user);
        // Додаємо станцію і передаємо на виконання на віддалений сервіс

        Gson gstation = gsonService.CreateGson();

        String strStation = gstation.toJson(station).toString();
        rjob.setRjobdata(strStation);
        // https://www.javaguides.net/2019/11/gson-localdatetime-localdate.html
        logger.info("Rjob : {}",strStation);
        Gson gson = gsonService.CreateGson();
        String strgson = gson.toJson(rjob).toString();
        logger.info("Str to rabbit {}",strgson);
        template.convertAndSend(queueNameRabbitMQ,gson.toJson(rjob).toString());
        return "redirect:/user/stations";
    }

    @GetMapping(value = "/user/controlstation")
    public String userControltStation(
            @RequestParam(value = "id", required = true) Long id,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Station mstation;
        mstation = stationService.GetStationById(id);
        if (mstation == null) {
            // Станцію створити не можемо. Показуємо про це повідомлення.
            logger.info("ClientHomeStationController:  Не можемо запустити станцію для користувача {}", user.getEmail());
            // TODO Відправити у форму повідомлення про неможливість створення станції та кинути клієнту месседж
            model.addAttribute("warning", "Не можемо знайти станцію (" + id.toString() + ") для користувача " + user.getEmail());
            return "redirect:/user/stations";
        }
        // користувач та станція знайдені. Працюємо зі станцією.

        model.addAttribute("formUserPSW",  new Users());
        model.addAttribute("contracts",  contractService.ListContractByUser(user));
        model.addAttribute("linkstation",  stationService.GetURLStation(mstation));
        model.addAttribute("station",  mstation);
        Boolean stationstateonline = stationService.GetStationRoomStatus(mstation.getRoomuuid());
        model.addAttribute("stationstateonline", stationstateonline) ;
        if (messangerService.GetChatRoomByUUID(mstation.getRoomuuid()) != null) {
            model.addAttribute("roomstart", messangerService.GetChatRoomByUUID(mstation.getRoomuuid()).getStartonline());
        } else {
            model.addAttribute("roomstart", null);
        }
        return "/user/controlstation";
    }

    @GetMapping(value = "/user/stationsetpsw")
    public String userSetStationPSW(
            @RequestParam(value = "id", required = true) Long id,
            @ModelAttribute Users formUserPSW,
            @ModelAttribute Station station,
            Model model ) {
        logger.info("Збереження паролю для станції.");
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        // встановлюємо пароль для адміна в панелі керування станцією



        Station mstation;
        mstation = stationService.GetStationById(id);

        model.addAttribute("formUserPSW",  new Users());
        model.addAttribute("contracts",  contractService.ListContractByUser(user));
        model.addAttribute("linkstation",  stationService.GetURLStation(mstation));
        model.addAttribute("station",  mstation);
        return "/user/controlstation";
    }

    @PostMapping(value = "/user/stationsave")
    public String userHomeStationSave(
            @ModelAttribute Station station,
            @ModelAttribute Users formUserPSW,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
//            logger.warn("userHomeStationSave: User not found. Redirect to main page");
            return "redirect:/";
        }

        if (station == null) {
            // Станцію створити не можемо. Показуємо про це повідомлення.
            logger.info("userHomeStationSave: Не можемо знайти станцію id={} для користувача {}", station.getId(), user.getEmail());
            model.addAttribute("warning", "Не можемо знайти станцію (" + station.getId().toString() + ") для користувача " + user.getEmail());
            // TODO Відправити у форму повідомлення про неможливість створення станції та кинути клієнту месседж
            // TODO зробити запис в журнал
            return "redirect:/user/stations";
        }

        Station nstation;
        if (station.getId() != null) {
            nstation = stationService.GetStationById(station.getId());
        } else {
            logger.info("userHomeStationSave:  Не можемо зберегти станцію id={} для користувача {}", station.getId(), user.getEmail());
            return "redirect:/user/stations";
        }


        nstation.setName(station.getName());

        nstation.setIcecastdescription(station.getIcecastdescription());
        nstation.setIcecastname(station.getIcecastname());
        nstation.setIcecastgenre(station.getIcecastgenre());
        nstation.setIcecastsite(station.getIcecastsite());
        nstation.setLastchangedate(new Date());
        stationService.saveStation(nstation);
        historyService.saveHistory(History_StationChange,
                nstation.getUuid().toString() + ": " +nstation.getLastchangedate().toString() + " Збережено станцію " + nstation.getUuid().toString(),
                user
        );
        String newpsw = formUserPSW.getPassword();
        if (!newpsw.isBlank()) {
            logger.info("Встановлюємо пароль");

            RPCJob rjob = new RPCJob();
            rjob.getJobchain().add(JOB_STATION_SETPASSWORD);
            try {
                MessageDigest md;
                md = MessageDigest.getInstance("MD5");
                md.update(newpsw.getBytes());
                byte[] digest = md.digest();
                user.setPassword(new String(Hex.encode(digest)));
            } catch (Exception e) {
                logger.info("Щось не так з MD5 :(");
                e.printStackTrace();
            }

            rjob.setUser(user);
            Gson gstation = gsonService.CreateGson();
            rjob.setRjobdata(gstation.toJson(station).toString());
            // https://www.javaguides.net/2019/11/gson-localdatetime-localdate.html
            Gson gson = gsonService.CreateGson();
            String strgson = gson.toJson(rjob).toString();
            template.convertAndSend(queueNameRabbitMQ,gson.toJson(rjob).toString());
            model.addAttribute("warning",  "Завдання на встановлення паролю відправлено на виконання.");
        }


        // TODO відправити повідомлення на сторінку
        model.addAttribute("success",  "Вашу станцію збережено в базу.");
        return "redirect:/user/stations";
    }



    @GetMapping(value = "/user/startstation")
    public String userStartStation(
            @RequestParam(value = "id", required = true) Long id,
            Model model ) {

        String message;

        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Station station = stationService.GetStationById(id);
        if (station == null) {
            // Станцію створити не можемо. Показуємо про це повідомлення.
            logger.info("userStartStation: Не можемо запустити станцію для користувача {}", user.getEmail());
            // TODO Відправити у форму повідомлення про неможливість запуску станції та кинути клієнту месседж
            return "redirect:/user/stations";
        }
        Contract contract = station.getContract();
        if (contract == null) {
            message = "Не можемо запустити станцію " + station.getUuid() + " для користувача " + user.getEmail() + " Перевірте, чи приєднана вона до контракту.";
            logger.info(message);
            model.addAttribute("success", message);
            return "redirect:/user/stations";
        }
        if ((station.getContract().getClientdetail().getAccount() < 0 ) && (station.getContract().getContractStatus() == EContractStatus.CONTRACT_PAY)) {
            // todo перевірити гроші на рахунку і проплачений термін
            logger.info("Не можемо запустити станцію " + station.getUuid() + " для користувача " + user.getEmail() + " Перевірте, чи достатньо коштів на рахунку");
            model.addAttribute("success", "Не можемо запустити станцію " + station.getUuid() + " для користувача " + user.getEmail() + " Перевірте, чи достатньо коштів на рахунку");
            return "redirect:/user/stations";
        }
        RPCJob rjob = new RPCJob();
        rjob.getJobchain().add(JOB_STATION_START);
        rjob.setUser(user);
        Gson gstation = gsonService.CreateGson();
        rjob.setRjobdata(gstation.toJson(station).toString());
        // https://www.javaguides.net/2019/11/gson-localdatetime-localdate.html
        Gson gson = gsonService.CreateGson();
        String strgson = gson.toJson(rjob).toString();
        template.convertAndSend(queueNameRabbitMQ,gson.toJson(rjob).toString());
//        return "/user/controlstation"+"/?id="+station.getId().toString();
        return "redirect:/user/stations";
    }

    @GetMapping(value = "/user/stopstation")
    public String userStopStation(
            @RequestParam(value = "id", required = true) Long id,

            Model model ) {

        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Station station = stationService.GetStationById(id);
        if (station == null) {
            return "redirect:/user/stations";
        }
        RPCJob rjob = new RPCJob();
        rjob.getJobchain().add(JOB_STATION_STOP);
        rjob.setUser(user);
        Gson gstation = gsonService.CreateGson();
        rjob.setRjobdata(gstation.toJson(station).toString());
        // https://www.javaguides.net/2019/11/gson-localdatetime-localdate.html
        Gson gson = gsonService.CreateGson();
        String strgson = gson.toJson(rjob).toString();
        template.convertAndSend(queueNameRabbitMQ,gson.toJson(rjob).toString());
        return "redirect:/user/stations";
    }

    @GetMapping(value = "/user/deletestation")
    public String userDelStation(
            @RequestParam(value = "id", required = true) Long id,

            Model model ) {

        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Station station = stationService.GetStationById(id);
        if (station == null) {
            model.addAttribute("error", "Йой! Щось пішло не так. Ми вже працюємо над цим!");
            return "redirect:/user/stations";
        }
        // todo Надіслати користувачу лист на підтвердження видалення станції
        // виконаємо це через RPC :)

        RPCJob rjob = new RPCJob();
        rjob.getJobchain().add(JOB_STATION_DELETE);
        rjob.setUser(user);
        Gson gstation = gsonService.CreateGson();
        rjob.setRjobdata(gstation.toJson(station).toString());
        // https://www.javaguides.net/2019/11/gson-localdatetime-localdate.html
        Gson gson = gsonService.CreateGson();
        String strgson = gson.toJson(rjob).toString();
        template.convertAndSend(queueNameRabbitMQ,gson.toJson(rjob).toString());
        model.addAttribute("success", "Вам надіслано листа на підтвердження видалення станції. Будь ласка, підтвердіть видалення");
        historyService.saveHistory(History_StationChange,
                station.getUuid().toString() + ":  Надіслано запит на видалення станції ",
                user
        );
        return "redirect:/user/stations";
    }

    @GetMapping(value = "/user/psstation")
    public String userPSStation(
            @RequestParam(value = "id", required = true) Long id,
            Model model ) {

//        logger.info("Create New station.");
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Station station = stationService.GetStationById(id);
        if (station == null) {
            // Станцію створити не можемо. Показуємо про це повідомлення.
            logger.info("userPSStation:  Не можемо перевірити стан станції для користувача {}", user.getEmail());
            return "redirect:/user/stations";
        }
        RPCJob rjob = new RPCJob();
        rjob.getJobchain().add(JOB_STATION_GET_PS);
        rjob.setUser(user);
        Gson gstation = gsonService.CreateGson();
        rjob.setRjobdata(gstation.toJson(station).toString());
        // https://www.javaguides.net/2019/11/gson-localdatetime-localdate.html
        Gson gson = gsonService.CreateGson();
        String strgson = gson.toJson(rjob).toString();
        template.convertAndSend(queueNameRabbitMQ,gson.toJson(rjob).toString());
        return "redirect:/user/stations";
    }
}
