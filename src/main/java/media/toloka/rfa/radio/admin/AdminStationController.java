package media.toloka.rfa.radio.admin;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import media.toloka.rfa.config.gson.service.GsonService;
import media.toloka.rfa.radio.admin.service.AdminService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.history.service.HistoryService;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.radio.newstoradio.model.NewsRPC;
import media.toloka.rfa.radio.post.service.PostService;
import media.toloka.rfa.radio.station.service.StationService;
import media.toloka.rfa.rpc.model.ERPCJobType;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Profile("Front")
@Controller
public class AdminStationController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private CreaterService createrService;

    @Autowired
    private PostService postService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private StationService stationService;

    @Autowired
    private RabbitTemplate template;

    @Autowired
    private GsonService gsonService;


    final Logger logger = LoggerFactory.getLogger(AdminStationController.class);


    // керування всіма станціями
    @GetMapping(value = "/admin/station")
    public String getAdminStation(
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        // Отримати статус всіх станцій
        List<Station> stationList = stationService.listAll();
        HashSet<String> setStationServer = new HashSet<String>();
        for (Station server : stationList) {
            setStationServer.add(server.getGuiserver());
        }
        logger.info ("Кількість серверів станцій в базі:{}",setStationServer.size());

        // Вибираємо сервери на яких розташовані станції

        // docker ps --format "table {{.ID}}\t{{.Names}}}"|grep playout|awk '{print substr($2,1,36)   }'

        NewsRPC rjob = new NewsRPC();
        rjob.setRJobType(ERPCJobType.JOB_GETRUNSTATIOM);

        Gson gson = gsonService.CreateGson();

        for (String curServer : setStationServer) {
            logger.info("====== Server: {} ",curServer);
//            List<String> response = (List<String>) template.convertSendAndReceive(curServer, gson.toJson(rjob).toString() );
            String response = (String) template.convertSendAndReceive(curServer+".callback", gson.toJson(rjob).toString() );
//            logger.info("Response RPC: {} ",response);
            Type listType = new TypeToken<List<String>>() {}.getType();
            List<String> listResponse = gson.fromJson(response, listType);

            HashSet<String> stationInCurrentServer = new HashSet<String>();
            for (String sStation : listResponse) {
                logger.info("Server: {} Station: {}",curServer,sStation);
                stationInCurrentServer.add(sStation);
            }
            // перегоняємо отримані станції, що працюють, в Set


            for (Station curStation : stationList) {
                if (curStation.getGuiserver().equals(curServer)) {
                    // Оновили статус станції на актуальний
                    if (stationInCurrentServer.contains(curStation.getUuid())) {
                        curStation.setStationstate(true);
                        stationService.saveStation(curStation);
                    } else {
                        curStation.setStationstate(false);
                        stationService.saveStation(curStation);
                    }
                }
            }
        }


        model.addAttribute("stationList", stationService.listAll() );

        return "/admin/station";
    }
}
