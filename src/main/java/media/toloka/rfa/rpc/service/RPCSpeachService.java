package media.toloka.rfa.rpc.service;

import com.google.gson.Gson;
import media.toloka.rfa.config.gson.service.GsonService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.history.service.HistoryService;
import media.toloka.rfa.radio.newstoradio.model.News;
import media.toloka.rfa.radio.newstoradio.service.NewsService;
import media.toloka.rfa.radio.station.service.StationService;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.rpc.model.RPCJob;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import static media.toloka.rfa.radio.model.enumerate.EHistoryType.History_NewsSendToTTS;
import static media.toloka.rfa.radio.model.enumerate.EHistoryType.History_StationCreate;

@Profile("tts")
@Service
public class RPCSpeachService {

//    @Autowired
//    private MessageService messageService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private StationService stationService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private StoreService storeService;

    @Autowired
    private NewsService newsService;

    @Autowired
    private GsonService gsonService;

//    @Autowired
//    private ServerRunnerService serverRunnerService;

    @Autowired
    RabbitTemplate template;


    @Value("${rabbitmq.queueTTS}")
    private String queueTTS;

    final Logger logger = LoggerFactory.getLogger(RPCSpeachService.class);

    public Long JobTTS (RPCJob rjob) {
//        logger.info(rjob);
        Long rc = 10000L;
        // витягли користувача
        Users user = rjob.getUser();
        // Витягнути новину з переданого gson
        String sUuidNews = rjob.getRjobdata();

        News news = newsService.GetByUUID(sUuidNews);

        rc =  newsService.PutTxtToTmp(sUuidNews, news.getNewsbody());
        if (rc != 0L) {
            newsService.deleteTmpFile(sUuidNews);
            return rc;
        }

        // Викликаємо перетворення
        rc = newsService.RunTxtToMp3(sUuidNews);
        if (rc != 0L) {
            newsService.deleteTmpFile(sUuidNews);
            return rc;
        }

        // Забираємо фінальний файл до сховища
        rc = newsService.PutMp3ToStore(sUuidNews);
        if (rc != 0L) {
            newsService.deleteTmpFile(sUuidNews);
            return rc;
        }

        // записуємо подію в журнал.
        historyService.saveHistory(History_NewsSendToTTS,
                sUuidNews + " Create TTS",
                user
                );

        newsService.deleteTmpFile(sUuidNews);
        return rc;
    }

    public void CompletedPartRPCJob (RPCJob rpcjob) {
        // 1. перевіряємо, чи на верхівці стеку завдання, яке ми виконали - якщо так, то видаляємо.
        // 2. якщо ще залишилися завдання, то ставимо в чергу на виконання.

        if (rpcjob.getJobchain().isEmpty()) {
            logger.info("Результат виконання Завдання: {}",rpcjob.getResultJobList().toString());
            return;
        }
        // якщо в черзі є елементи, то відправляємо на виконання вибираючи з черги черговий елемент.

//        rpcjob.setRJobType(rpcjob.getJobchain().poll()); // set job type
        Gson gson = gsonService.CreateGson();
        String strgson = gson.toJson(rpcjob).toString();
        template.convertAndSend(queueTTS,gson.toJson(rpcjob).toString());
        return;
    }

}
