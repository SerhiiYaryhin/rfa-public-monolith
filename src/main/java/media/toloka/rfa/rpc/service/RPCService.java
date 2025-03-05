package media.toloka.rfa.rpc.service;

import com.google.gson.Gson;
import media.toloka.rfa.service.RfaService;
import media.toloka.rfa.config.gson.service.GsonService;
import media.toloka.rfa.radio.history.service.HistoryService;
//import media.toloka.rfa.radio.message.service.MessageService;
import media.toloka.rfa.radio.model.enumerate.EServerPortType;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.radio.station.service.PoolPortsService;
import media.toloka.rfa.radio.station.service.StationService;
import media.toloka.rfa.rpc.model.RPCJob;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static media.toloka.rfa.radio.model.enumerate.EHistoryType.History_StationCreate;

@Service
public class RPCService {

//    @Autowired
//    private MessageService messageService;

    @Autowired
    private PoolPortsService poolPortsService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private StationService stationService;

    @Autowired
    private GsonService gsonService;

    @Autowired
    private ServerRunnerService serverRunnerService;

    @Autowired
    RabbitTemplate template;

    @Autowired
    private RfaService rfaService;

    @Value("${rabbitmq.queue}")
    private String queueName;

    final Logger logger = LoggerFactory.getLogger(RPCService.class);

    public void SetStationDBName(Station st) {
        while (true) {
            String  rstring = rfaService.GetRandomString(16);
            if (stationService.GetStationDBName(rstring) == null) {
                st.setDbname(rstring);
                return;
            }
        }
    }



    public Long JobCreateStation (RPCJob rjob) {
//        logger.info(rjob);
        // витягли користувача
        Users user = rjob.getUser();
        // Витягнути станцію з переданого gson
        String sStation = rjob.getRjobdata();
        Gson gStation = gsonService.CreateGson();
        Station Radio = gStation.fromJson(sStation, Station.class);

        Station newRadio = stationService.GetStationById(Radio.getId());
        // генеруємо випадковий рядок символів для імені бази
        SetStationDBName(newRadio);


        // саме тут створюємо новий обʼєкт - радіостанція
        newRadio = poolPortsService.AttachPort(user,newRadio, EServerPortType.PORT_MAIN);
//        newRadio.setUuid(UUID.randomUUID().toString()); //
        // Сохраніть станцію с портом (Занять порт в пуле портов)
        stationService.saveStation(newRadio);
        newRadio = poolPortsService.AttachPort(user,newRadio, EServerPortType.PORT_SHOW);
        stationService.saveStation(newRadio);
        newRadio = poolPortsService.AttachPort(user,newRadio, EServerPortType.PORT_WEB);
        newRadio.setName(newRadio.getUuid());
        stationService.saveStation( newRadio);
        // записуємо подію в журнал.
        String sCreateDate = newRadio.getCreatedate().toString();
        String suuid = newRadio.getUuid().toString();
        historyService.saveHistory(History_StationCreate,
                newRadio.getCreatedate().toString() + " Create " + newRadio.getUuid().toString(),
                user
                );

        Gson gstation = gsonService.CreateGson();
        rjob.setRjobdata(gstation.toJson(newRadio).toString());
//        rjob.setRJobType(JOB_STATION_ALLOCATE);
//        Gson gson = gsonService.CreateGson();
////        String strgson = gson.toJson(rjob).toString();
//        template.convertAndSend(queueName,gson.toJson(rjob).toString());
        // подивилися, чи є у нього угода. Якщо немає контракту то створили безкоштовний
            // відправили повідомлення про створення контракту
        // подивилися, чи є в угоді станція. Якщо немає то створили радіостанцію на безкоштовному пакеті
            // відправили повідомлення про створення радіостанції
            // якщо в безкоштовній угоді є радіостанція, то повідомлення, що вже є безкоштовна станція
        // створили станцію
            // відправили повідомлення про створення станції
//        serviceMessage.SendMessageToUser(null, user, msg);
        return 0L;
    }
}
