package media.toloka.rfa.rpc;


import com.google.gson.Gson;
import media.toloka.rfa.config.gson.service.GsonService;
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

Logger logger = LoggerFactory.getLogger(RPCTTSListener.class);

    @RabbitListener(queues = "${rabbitmq.queueTTS}")
    public void processedFromFront(String message) {
        Long rc = 0L;
        Gson gson = gsonService.CreateGson();
        RPCJob rjob = gson.fromJson(message, RPCJob.class);


//        logger.info("+++++++++++++++++  Recive message from QUEUES.");
        ERPCJobType curJob = rjob.getJobchain().poll();
        switch (curJob) {
//        switch (rjob.getRJobType()) {
            case JOB_TTS:  // Заповнюємо базу необхідною інформацією
                logger.info("+++++++++++++++++ START JOB_TTS");
                rc = rpcSpeachService.JobTTS(rjob);
                rjob.getResultJobList().add(new ResultJob(rc, curJob));
                logger.info("+++++++++++++++++ END JOB_TTS");
                break;
            case JOB_STT:
                logger.info("======= {}    {}", rjob.getRJobType(), rjob.getRjobdata());
                break;
            default:
                logger.info("RPC Listener CASE DEFAULT: Якась дивна команда прелетіла ======= {}    {}", rjob.getRJobType(), rjob.getRjobdata());
                break;
        }
        rpcSpeachService.CompletedPartRPCJob(rjob);

    }
}