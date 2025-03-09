package media.toloka.rfa.rpc.service;

import com.google.gson.Gson;
import media.toloka.rfa.config.gson.service.GsonService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.history.service.HistoryService;
import media.toloka.rfa.radio.newstoradio.model.News;
import media.toloka.rfa.radio.newstoradio.model.NewsRPC;
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

import java.io.*;
import java.util.Map;

import static media.toloka.rfa.radio.model.enumerate.EHistoryType.History_NewsSendToTTS;
import static media.toloka.rfa.radio.store.model.EStoreFileType.STORE_TTS;
import static media.toloka.rfa.rpc.model.ERPCJobType.JOB_TTS_FILES_READY;

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

    //    @Value("${media.toloka.rfa.server.runTxtToMp3}")
    private String runTxtToMp3 = "~/bin/runTxtToMp3.sh ";

    @Value("${rabbitmq.queueTTS}")
    private String queueTTS;

    @Value("${media.toloka.rfa.server.libretime.storeserver}")
    private String storeserver;

    final Logger logger = LoggerFactory.getLogger(RPCSpeachService.class);

    public Long JobTTS(NewsRPC rjob) {
        Long rc = 10000L;
        // Витягнути новину з переданого gson
        String sUuidNews = rjob.getNewsUUID();
        News news = newsService.GetByUUID(sUuidNews);
        // витягли користувача
        Users user = news.getClientdetail().getUser();


        rc = PutTxtToTmp(sUuidNews, news.getNewsbody());
        logger.info("==== PutTxtToTmp rc:{}", rc);
        if (rc != 0L) {
            deleteTmpFile(sUuidNews);
            return rc;
        }

        // Викликаємо перетворення
        rc = RunTxtToMp3(sUuidNews);
        if (rc != 0L) {
            deleteTmpFile(sUuidNews);
            return rc;
        }

        // Забираємо фінальний файл до сховища
        rc = SendJobForPutMp3ToStore(rjob);
        if (rc != 0L) {
            deleteTmpFile(sUuidNews);
            return rc;
        }

        // записуємо подію в журнал.
        historyService.saveHistory(History_NewsSendToTTS,
                sUuidNews + " Create TTS",
                user
        );

//        deleteTmpFile(sUuidNews);
        return rc;
    }

    public Long PutTxtToTmp(String sUuidNews, String nbody) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter("/tmp/" + sUuidNews + ".tts"));
            writer.write(nbody);

        } catch (IOException e) {
            logger.info("Помилка при запису tts файлу.");
            return 1L;
        } finally {
            try {
                if (writer != null) writer.close();
            } catch (IOException e) {
                logger.info("Помилка при закритті tts файлу.");
                return 2L;
            }
        }
        return 0L;
    }

    public void deleteTmpFile(String sUuidNews) {
        String patch;
        patch = "/tmp/" + sUuidNews + ".tts";
        File file = new File(patch);
        if (file.delete()) {
            logger.info("File deleted successfully:{}", patch);
        } else {
            logger.info("Failed to delete the file:", patch);
        }

        patch = "/tmp/" + sUuidNews + ".wav";
        file = new File(patch);
        if (file.delete()) {
            logger.info("File deleted successfully:{}", patch);
        } else {
            logger.info("Failed to delete the file:{}", patch);
        }

        patch = "/tmp/" + sUuidNews + ".mp3";
        file = new File(patch);
        if (file.delete()) {
            logger.info("File deleted successfully:{}", patch);
        } else {
            logger.info("Failed to delete the file:{}", patch);
        }

    }

    public Long RunTxtToMp3(String sUuidNews) {
        Long rc = 129L;
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", runTxtToMp3 + sUuidNews);
        Map<String, String> env = pb.environment();

        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info(line);
            }
            int exitcode = p.waitFor();
            rc = Long.valueOf(exitcode);
        } catch (IOException e) {
            logger.warn(" Щось пішло не так при виконанні завдання в операційній системі");
            e.printStackTrace();
        } catch (InterruptedException e) {
            logger.warn(" Щось пішло не так при виконанні завдання (p.waitFor) InterruptedException");
            e.printStackTrace();
        }
        //================================================================
        // https://www.javaguides.net/2019/11/gson-localdatetime-localdate.html
        return rc;
    }

    public Long SendJobForPutMp3ToStore(NewsRPC rjob) {
        // Move file from TTS server
        rjob.getTts().setUser(System.getenv("USER"));
        rjob.getTts().setServer("tts.rfa");
        rjob.getFront().setServer(storeserver);
        rjob.setRJobType(JOB_TTS_FILES_READY);

        Gson gson = gsonService.CreateGson();
        template.convertAndSend(rjob.getFront().getServer(), gson.toJson(rjob).toString());
        logger.info(rjob.toString());
//
//        String patch = "/tmp/" + rjob.getNewsUUID() + ".mp3";
//        File initialFile = new File(patch);
//        InputStream targetStream = null;
//        try {
//            targetStream = new FileInputStream(initialFile);
//        } catch (FileNotFoundException e) {
//            logger.info("==== Щось пішло не так! Не можу знайти результат TTS. {}", "/tmp/" + rjob.getNewsUUID() + ".mp3");
//        }
//        String storeUUID = storeService.PutFileToStore(targetStream, rjob.getNewsUUID() + ".mp3", newsService.GetByUUID(rjob.getNewsUUID()).getClientdetail(), STORE_TTS);
//        newsService.GetByUUID(rjob.getNewsUUID()).setStorespeach(storeService.GetStoreByUUID(storeUUID));
//        logger.info("uploaded file " + rjob.getNewsUUID() + ".mp3");

        return 0L;
    }


}
