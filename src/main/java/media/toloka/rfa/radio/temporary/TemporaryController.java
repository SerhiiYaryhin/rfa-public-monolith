package media.toloka.rfa.radio.temporary;

import com.google.gson.Gson;
import media.toloka.rfa.config.gson.service.GsonService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.client.service.ClientService;
//import media.toloka.rfa.radio.message.service.MessageService;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.radio.station.service.StationService;
import media.toloka.rfa.rpc.RPCListener;
//import media.toloka.rfa.rpc.model.ERPCJobType;
import media.toloka.rfa.rpc.model.RPCJob;
import media.toloka.rfa.rpc.service.ServerRunnerService;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static media.toloka.rfa.rpc.model.ERPCJobType.*;
import static media.toloka.rfa.rpc.model.ERPCJobType.JOB_STATION_STOP;

@Controller
public class TemporaryController {

    @Value("${rabbitmq.queue}")
    private String queueNameRabbitMQ;

    @Autowired
    private ServerRunnerService serverRunnerService;

    @Autowired
    private StationService stationService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private GsonService gsonService;

    @Autowired
    RabbitTemplate template;

    @Value("${rabbitmq.queue}")
    private String queueName;

    Logger logger = LoggerFactory.getLogger(RPCListener.class);


    // todo пеерйти на використання UUID замість ID
    // todo Зʼясувати, чи використовується ця процедура.
    @GetMapping(value = "/user/stationstatus/{id}")
    public String userHomeStation(
            Model model ) {
        return "/temporary/stationstatus";
    }

    @GetMapping(value = "/user/temporaryid")
    public String PostDocumentEdit(
            @RequestParam(value = "id", required = true) Long id,
            Model model
    ) {
        Users user = clientService.getCurrentUser();
        if (user == null) {
//            logger.warn("User not found. Redirect to main page");
            return "redirect:/login";
        }

//        messageService.setQuantityMessage(clientService.getClientDetail(clientService.getCurrentUser()));
        Station station = stationService.getStationById(id);
        Clientdetail cd = station.getClientdetail();

        // відправляємо завдання на створення радіостанції.
        RPCJob rjob = new RPCJob();
        rjob.setRJobType(JOB_STATION_ALLOCATE); // set job type
        rjob.setUser(user);
        if (station == null) {
            // Станцію створити не можемо. Показуємо про це повідомлення.
            logger.info("Не можемо створити станцію для користувача {}", user.getEmail());
            // TODO Відправити у форму повідомлення про неможливість створення станції та кинути клієнту месседж
            return "redirect:/user/stations";
        }



//        RPCJob rjob = new RPCJob();
//        rjob.getJobchain().add(JOB_STATION_CREATE);
        rjob.getJobchain().add(JOB_STATION_ALLOCATE);
        rjob.getJobchain().add(JOB_STATION_PREPARE_NGINX);
        rjob.getJobchain().add(JOB_STATION_LIBRETIME_MIGRATE);
        rjob.getJobchain().add(JOB_STATION_START);
        rjob.getJobchain().add(JOB_STATION_STOP);
//        rjob.setRJobType(JOB_STATION_CREATE); // Створюємо необхідну інформацію в базі для станції
        rjob.setUser(user);
        // Додаємо станцію і передаємо на виконання на віддалений сервіс

        Gson gstation = gsonService.CreateGson();
        rjob.setRjobdata(gstation.toJson(station).toString());
        // https://www.javaguides.net/2019/11/gson-localdatetime-localdate.html
        Gson gson = gsonService.CreateGson();
        String strgson = gson.toJson(rjob).toString();
        template.convertAndSend(queueNameRabbitMQ,gson.toJson(rjob).toString());
//
//
//        Gson gstation = gsonService.CreateGson();
//        rjob.setRjobdata(gstation.toJson(station).toString());
//        // https://www.javaguides.net/2019/11/gson-localdatetime-localdate.html
//        Gson gson = gsonService.CreateGson();
//        String strgson = gson.toJson(rjob).toString();
//        template.convertAndSend(queueName,gson.toJson(rjob).toString());

        return "redirect:/user/temporary";
//        return "redirect:/user/temporary";
    }


}
