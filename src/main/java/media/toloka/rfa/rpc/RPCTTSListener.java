package media.toloka.rfa.rpc;


import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import media.toloka.rfa.config.gson.service.GsonService;
import media.toloka.rfa.radio.newstoradio.model.NewsRPC;
import media.toloka.rfa.rpc.model.ERPCJobType;
import media.toloka.rfa.rpc.model.RPCJob;
import media.toloka.rfa.rpc.model.ResultJob;
import media.toloka.rfa.rpc.service.RPCSpeachService;
import media.toloka.rfa.rpc.service.ServerRunnerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("tts")
@Component
public class RPCTTSListener {

//    @Autowired
//    private RPCService serviceRPC;

//    @Autowired
//    private ServerRunnerService serverRunnerService;

    @Autowired
    private RPCSpeachService rpcSpeachService;

    @Autowired
    RabbitTemplate template;

    @Autowired
    private GsonService gsonService;

    @Value("${rabbitmq.queueTTS}")
    private String queueTTS;

Logger logger = LoggerFactory.getLogger(RPCTTSListener.class);

    @RabbitListener(queues = "${rabbitmq.queueTTS}")
    public void processedFromFront(String message) {
        Long rc = 0L;
        Gson gson = gsonService.CreateGson();

        NewsRPC rjob = null;
        try {
            rjob = gson.fromJson(message, NewsRPC.class);
        } catch (JsonParseException e) {
            logger.info("Помилка gson");
            logger.info("\n rabbitmq message:"+message);
            logger.error(e.getMessage());
            return;
        }

        logger.info("+++++++++++++++++  Recive message from rabbitmq.queueTTS ={}.",queueTTS);
        ERPCJobType curJob = rjob.getRJobType();
        switch (curJob) {
//        switch (rjob.getRJobType()) {
            case JOB_TTS:  // Заповнюємо базу необхідною інформацією
                logger.info("+++++++++++++++++ START JOB_TTS");
                rc = rpcSpeachService.JobTTS(rjob);
//                rjob.getResultJobList().add(new ResultJob(rc, curJob));
                logger.info("+++++++++++++++++ END JOB_TTS");
                break;
            case JOB_STT:
                logger.info("======= {}", rjob.getRJobType());
                break;
            default:
                logger.info("RPC Listener CASE DEFAULT: Якась дивна команда прелетіла ======= {}", rjob.getRJobType());
                break;
        }
        //rpcSpeachService.CompletedPartRPCJob(rjob);

    }
}