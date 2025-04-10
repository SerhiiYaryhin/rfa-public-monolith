package media.toloka.rfa.radio.stt;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import media.toloka.rfa.config.gson.service.GsonService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.history.service.HistoryService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.radio.newstoradio.model.*;
import media.toloka.rfa.radio.station.service.StationService;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.stt.model.ESttStatus;
import media.toloka.rfa.radio.stt.model.Stt;
import media.toloka.rfa.radio.stt.service.STTBackServerService;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static media.toloka.rfa.radio.newstoradio.model.ENewsStatus.NEWS_STATUS_CREATE;
import static media.toloka.rfa.radio.stt.model.ESttStatus.STT_STATUS_CREATE;
import static media.toloka.rfa.rpc.model.ERPCJobType.JOB_STT;
import static media.toloka.rfa.rpc.model.ERPCJobType.JOB_TTS;

@Controller
public class STTHome {

    @Value("${rabbitmq.queueTTS}")
    private String queueTTS;

    @Value("${rabbitmq.queueSTT}")
    private String queueSTT;
    @Value("${media.toloka.rfa.station.basename}")
    private String baseSiteAddress;

    @Value("${media.toloka.rfa.server.toradiosever.name}")
    private String toradiosevername;
    @Value("${media.toloka.rfa.server.toradiosever.user}")
    private String toradioseveruser;
    @Value("${media.toloka.rfa.server.toradiosever.psw}")
    private String toradioseverpsw;
    @Value("${media.toloka.rfa.server.toradiosever.queue}")
    private String toRadioServerQueue;
    @Value("${media.toloka.rfa.server.libretime.guiserver}")
    private String localGuiServer;

    @Autowired
    private RabbitTemplate template;

    @Autowired
    private GsonService gsonService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private StoreService storeService;

    @Autowired
    private STTBackServerService sttBackServerService;

    @Autowired
    private HistoryService historyService;

    final Logger logger = LoggerFactory.getLogger(STTHome.class);

    /// видаляємо трек

    /// видаляємо запис
    @GetMapping(value = "/stt/deletestt/{scurpage}/{uuidstt}")
    public String userDeleteStt(
            @PathVariable String uuidstt,
            @PathVariable String scurpage,
            Model model) {

        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(user);

        Stt stt = sttBackServerService.GetByUUID(uuidstt);


        if (stt == null) {

            return "/stt/home/0";
        }



//        model.addAttribute("liststation", listStation);
//        model.addAttribute("categorys", category);
        model.addAttribute("curstt", uuidstt);
        model.addAttribute("currentPage", scurpage);

        Long rc = 0L;
        if (stt != null) rc = sttBackServerService.deleteStt(uuidstt);
        if (rc == 0L) model.addAttribute("success", "Файл з голосом успішно видалено");
        else model.addAttribute("error", "Файл з голосом не видалено");
        return "redirect:/stt/home/"+scurpage;
    }

    /// відправляємо текст на перетворення
    @GetMapping(value = "/stt/sttprepare/{scurpage}/{uuidstt}")
    public String ttsprepare(
            @PathVariable String uuidstt,
            @PathVariable String scurpage,
            Model model) {

        Users user = clientService.GetCurrentUser();
        Clientdetail clientdetail = clientService.GetClientDetailByUser(user);
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(user);

        Stt curnews = sttBackServerService.GetByUUID(uuidstt);
        if (curnews != null) {
            // знайшли новину. Відправляємо на tts
            NewsRPC rjob = new NewsRPC();
            rjob.setRJobType(JOB_STT);
            rjob.getFront().setUser(System.getenv("USER"));
            rjob.setNewsUUID(curnews.getUuid());

            rjob.setRc(1024L);

            Gson gson = gsonService.CreateGson();
            template.convertAndSend(queueTTS, gson.toJson(rjob).toString());
            model.addAttribute("success", "Завдання перетворення тексту в голос надіслано на обробку.");
            curnews.setStatus(ESttStatus.STT_STATUS_SEND);
            curnews.setDatechangestatus(new Date());
            sttBackServerService.Save(curnews);

        } else {
            model.addAttribute("error", "<b>Щось пішло не так - не знайшли новину "
                    + ".Завдання перетворення тексту в голос не надіслано на обробку.</b>");
            logger.info("==== NEWS ttsprepare: Щось пішло не так - не знайшли новину {}",uuidstt);
        }
        // формуємо інформацію для відображення
        Integer curpage = 0;

// Пейджинг для сторінки
        Page pageStore = sttBackServerService.GetNewsPageByClientDetail(curpage, 10, cd);
        List<News> viewList = pageStore.stream().toList();

        model.addAttribute("totalPages", pageStore.getTotalPages());
        model.addAttribute("currentPage", scurpage);
        model.addAttribute("linkPage", "/creater/tracks/");
        model.addAttribute("viewList", viewList);

        return "redirect:/stt/home/" + scurpage;
    }

    /// відображаємо сторінку з новинами
    @GetMapping(value = "/stt/home/{cPage}")
    public String GetNewsHome(
            @PathVariable String cPage,
            Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(user);
        Integer curpage = Integer.parseInt(cPage);

// Пейджинг для сторінки
        Page pageStore = sttBackServerService.GetNewsPageByClientDetail(curpage, 10, cd);
        List<Stt> viewList = pageStore.stream().toList();
        List<Stt> sttList = sttBackServerService.GetListNewsByCd(cd);
//        Boolean runTTS = false;
//        for (Stt runnews : sttList) {
//            if (runnews.getStatus() == ENewsStatus.NEWS_STATUS_SEND) {
//                runTTS = true;
//                model.addAttribute("success", "У черзі на перетворення тексту в голос є завдання. Зараз з новинами нічого не можна робити. Трошки зачекайте та оновіть сторінку через пару хвилин.");
//            }
//        }

//        model.addAttribute("runstatus", runTTS);
        model.addAttribute("totalPages", pageStore.getTotalPages());
        model.addAttribute("currentPage", curpage);
        model.addAttribute("linkPage", "/creater/tracks/");
        model.addAttribute("viewList", viewList);

        return "/stt/home";
    }

    /// Створюємо або редагуємо новину
    @GetMapping(value = "/stt/editstt/{scurpage}/{uuid}")
    public String GetEditNews(
            @PathVariable String uuid, // uuid або запису stt, або запису storage
            @PathVariable String scurpage,
            Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(user);

        Stt curstt = sttBackServerService.GetByUUID(uuid);
        if (curstt == null) {
            curstt = new Stt();
            curstt.setClientdetail(cd);
            curstt.setStorespeach(storeService.GetStoreByUUID(uuid));
        }
//        curstt.setClientdetail(cd);

//        List<ENewsCategory> category = Arrays.asList(ENewsCategory.values());


//        List<ENewsVoice> voices = Arrays.asList(ENewsVoice.values());

//        model.addAttribute("voices", voices);
//        model.addAttribute("categorys", category);
        model.addAttribute("curstt", curstt);
        model.addAttribute("currentPage", scurpage);

        return "/stt/editstt";
    }

    /// зберігаємо створену або відредаговану новину
    @PostMapping(value = "/stt/editstt/{pagelist}")
    public String newsCreateEditNews(
//            @PathVariable String uuidNews,
            @PathVariable String pagelist,
            @ModelAttribute Stt fStt,
            Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(user);

//        News stt = newsService.GetByUUID(uuidNews);

        Stt stt = null;
        if (fStt.getUuid() != null) {
            stt = sttBackServerService.GetByUUID(fStt.getUuid());
        }
        Boolean type;
        if (stt != null) {
            stt.setTitle(fStt.getTitle());
            type = false;
        } else {
            stt = new Stt();
            stt.setClientdetail(cd);
            stt.setTitle(fStt.getTitle());
            stt.setStorespeach(fStt.getStorespeach());
            stt.setId(System.currentTimeMillis());
            stt.setUuid(UUID.randomUUID().toString());
            type = true;
        }

//        logger.info(stt.toString());
        if (stt != null) sttBackServerService.Save(stt);

        if (type) return "redirect:/stt/home/0";
        return "redirect:/stt/home/" + pagelist;

    }

}
