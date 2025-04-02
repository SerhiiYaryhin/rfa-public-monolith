package media.toloka.rfa.rpc;
/// RPC для сервера з радіо

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import media.toloka.rfa.config.gson.service.GsonService;
import media.toloka.rfa.radio.newstoradio.model.NewsRPC;
import media.toloka.rfa.radio.newstoradio.service.NewsBackServerService;
import media.toloka.rfa.rpc.model.ERPCJobType;
import media.toloka.rfa.rpc.service.ServerRunnerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

@Profile("Front")
@Component
public class BackRPCResponce {

    @Autowired
    RabbitTemplate template;

    @Autowired
    private GsonService gsonService;

    @Autowired
    private NewsBackServerService NewsBackServerService;

    @Autowired
    private ServerRunnerService serverRunnerService;


    Logger logger = LoggerFactory.getLogger(BackRPCResponce.class);

    @RabbitListener(queues = "${media.toloka.rfa.server.libretime.guiserver}"+".callback")
    public String processedFromFront(String message) {
        Long rc = 0L;
        Gson gson = gsonService.CreateGson();
        NewsRPC rjob = gson.fromJson(message, NewsRPC.class);

        ERPCJobType curJob = rjob.getRJobType();
        if (curJob == null) {
            logger.info("RPC Server Listener: Якийсь дивний json рядок прелетів ======= {}", message);
            return "";
        }
        switch (curJob) {
//        switch (rjob.getRJobType()) {
            case JOB_GETRUNSTATIOM:
                logger.info("+++++++++++++++++ START JOB_GETRUNSTATIOM");
                List<String> lrc = serverRunnerService.StationGetRunStation(rjob);
                //rjob.setRc(rc);
                logger.info("+++++++++++++++++ END JOB_GETRUNSTATIOM ");
                Type listType = new TypeToken<List<String>>() {}.getType();
                Gson rgson = new Gson();
                String json = rgson.toJson(lrc, listType);
                return json;
            default:
                logger.info("News RPC Listener CASE DEFAULT: Якась дивна команда прелетіла ======= {}", rjob.getRJobType());
                return "+++++++++";
        }
    }
}
