package media.toloka.rfa.rpc;
/// RPC для сервера з радіо

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import media.toloka.rfa.config.gson.service.GsonService;
import media.toloka.rfa.radio.newstoradio.model.NewsRPC;
import media.toloka.rfa.radio.newstoradio.service.NewsBackServerService;
import media.toloka.rfa.radio.stt.model.SttRPC;
import media.toloka.rfa.radio.stt.service.STTBackServerService;
import media.toloka.rfa.rpc.model.ERPCJobType;
import media.toloka.rfa.rpc.service.ServerRunnerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Profile("Front")
@Component
public class BackRPCRunner {

    @Autowired
    RabbitTemplate template;

    @Autowired
    private GsonService gsonService;

    @Autowired
    private NewsBackServerService NewsBackServerService;

    @Autowired
    private STTBackServerService sttBackServerService;

    @Autowired
    private ServerRunnerService serverRunnerService;


    Logger logger = LoggerFactory.getLogger(BackRPCRunner.class);

    @RabbitListener(queues = "${media.toloka.rfa.server.libretime.guiserver}")
    public void processedFromFront(String message) {
        logger.info("\nBackRPCRunner. RPC Server Listener:  {}", message);

        Long rc = 0L;
        Gson gson = gsonService.CreateGson();

        // Парсимо строку в JsonObject (через JsonElement)
        String namejob = "";
        ERPCJobType curJob;
        JsonElement element = JsonParser.parseString(message);


        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            // Отримуємо конкретне поле
            namejob = obj.get("rJobType").getAsString();
        }
        curJob = gson.fromJson(namejob, ERPCJobType.class);


        NewsRPC rjob = gson.fromJson(message, NewsRPC.class);

//        curJob = rjob.getRJobType();
//        ERPCJobType curJob = rjob.getRJobType();
        if (curJob == null) {
            logger.info("\nRPC Server Listener: Якийсь дивний json рядок прелетів ======= {}", message);
            return;
        }
        switch (curJob) {
//        switch (rjob.getRJobType()) {
            case JOB_TTS_FILES_READY:  // Перетягуємо файли після TTS
                NewsRPC lrjob = gson.fromJson(message, NewsRPC.class);
                // текстовий файл перетворено за аудіо
                logger.info("\n+++++++++++++++++ START JOB_TTS_FILES_READY");
                rc = NewsBackServerService.GetMp3FromTts(lrjob);
                rjob.setRc(rc);
                logger.info("\n+++++++++++++++++ END JOB_TTS_FILES_READY");
                break;
            case JOB_STT_FILES_READY:
                logger.info("\n+++++++++++++++++ START JOB_STT_FILES_READY");
                SttRPC sttrjob = gson.fromJson(message, SttRPC.class);
//                List<String> lrc =
                logger.info("\nRun time server.Start: {} Stop: {}", sttrjob.getStartjob(), sttrjob.getEndjob());
                sttBackServerService.StationGetSttResult(sttrjob);
                //rjob.setRc(rc);
                logger.info("\n+++++++++++++++++ END JOB_STT_FILES_READY");
                break;
            default:
                logger.info("\nNews RPC Listener CASE DEFAULT: Якась дивна команда прелетіла ======= {}", rjob.getRJobType());
                break;
        }
    }
}
