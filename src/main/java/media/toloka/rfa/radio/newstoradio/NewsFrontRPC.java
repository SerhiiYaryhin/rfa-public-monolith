package media.toloka.rfa.radio.newstoradio;

import com.google.gson.Gson;
import media.toloka.rfa.config.gson.service.GsonService;
import media.toloka.rfa.radio.newstoradio.model.NewsRPC;
import media.toloka.rfa.radio.newstoradio.service.NewsService;
import media.toloka.rfa.rpc.model.ERPCJobType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("Front")
@Component
public class NewsFrontRPC {

    @Autowired
    RabbitTemplate template;

    @Autowired
    private GsonService gsonService;

    @Autowired
    private NewsService newsService;

    Logger logger = LoggerFactory.getLogger(NewsFrontRPC.class);

    @RabbitListener(queues = "${media.toloka.rfa.server.libretime.guiserver}")
    public void processedFromFront(String message) {
        Long rc = 0L;
        Gson gson = gsonService.CreateGson();
        NewsRPC rjob = gson.fromJson(message, NewsRPC.class);

        ERPCJobType curJob = rjob.getRJobType();
        switch (curJob) {
//        switch (rjob.getRJobType()) {
            case JOB_TTS_FILES_READY:  // Перетягуємо файли після TTS
                logger.info("+++++++++++++++++ START JOB_TTS_FILES_READY");
                // rc = serviceRPC.JobCreateStation(rjob); // from Client Page. Next step
//                rc = newsService.GetMp3FromTTS(rjob);
                rc += newsService.GetMp3FromTts(rjob);
                        rjob.setRc(rc);
                logger.info("+++++++++++++++++ END JOB_TTS_FILES_READY");
                break;
            default:
                logger.info("News RPC Listener CASE DEFAULT: Якась дивна команда прелетіла ======= {}", rjob.getRJobType());
                break;
        }
    }
}
