package media.toloka.rfa.radio.admin;

///  Контролер адміністратора для керування станціями - станом та статусом

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

        List<Station> stationList = stationService.listAll();
        // отримуємо перелік серверів на яких можуть бути запущені станції
        HashSet<String> setStationServer = new HashSet<String>();
        for (Station server : stationList) {
            setStationServer.add(server.getGuiserver());
        }
        // команда, яка повертає в консоль перелік запущених станцій
        // docker ps --format "table {{.ID}}\t{{.Names}}}"|grep playout|awk '{print substr($2,1,36)   }'
        // Отримати стан станцій на серверах
        NewsRPC rjob = new NewsRPC();
        rjob.setRJobType(ERPCJobType.JOB_GETRUNSTATIOM);
        Gson gson = gsonService.CreateGson();
        // цикл по серверах на яких розташовані станції
        for (String curServer : setStationServer) {
            logger.info("====== Server: {} ",curServer);
            // todo Відпрацювати таймаут для відповіді
            String response = (String) template.convertSendAndReceive(curServer+".callback", gson.toJson(rjob).toString() );
            // забрали з відповіді масив String uuid станцій, які працюють
            List<String> listResponse = gson.fromJson(response, new TypeToken<List<String>>() {}.getType());

            // отримуємо в Set перелік uuid станцій, що працюють.
            HashSet<String> stationInCurrentServer = new HashSet<String>();
            for (String sStation : listResponse) {
                logger.info("Server: {} Station: {}",curServer,sStation);
                stationInCurrentServer.add(sStation);
            }

            // Перевіряємо відповідність статусів станцій в базі на відповідність запущеним.
            for (Station curStation : stationList) {
                if (curStation.getGuiserver().equals(curServer)) {
                    // todo не оновлювати стан коли він актуальний.
                    // Оновили статус станції на актуальний в базі
//                    if (stationInCurrentServer.contains(curStation.getUuid())) {
//                        if (curStation.getStationstate() != true)
//                        {
//                            // Оновили статус та зберігли у базі
//                            curStation.setStationstate(true);
//                            stationService.saveStation(curStation);
//                        }
//                    }
//                    else {
//                        if (curStation.getStationstate() != false) {
//                            // Оновили статус та зберігли у базі
//                            curStation.setStationstate(false);
//                            stationService.saveStation(curStation);
//                        }
//                    }
                    curStation.setStationstate(stationInCurrentServer.contains(curStation.getUuid()));
                }
            }
        }
        // тепер ми отримуємо з бази перелік всіх станцій з актуальним статусом.
        model.addAttribute("stationList", stationService.listAll() );

        return "/admin/station";
    }
}
