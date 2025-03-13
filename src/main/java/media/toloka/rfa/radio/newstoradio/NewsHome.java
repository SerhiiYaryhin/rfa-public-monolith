package media.toloka.rfa.radio.newstoradio;


import com.google.gson.Gson;
import media.toloka.rfa.config.gson.service.GsonService;
import media.toloka.rfa.radio.client.ClientHomeController;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.history.service.HistoryService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.radio.newstoradio.model.ENewsCategory;
import media.toloka.rfa.radio.newstoradio.model.ENewsStatus;
import media.toloka.rfa.radio.newstoradio.model.News;
import media.toloka.rfa.radio.newstoradio.model.NewsRPC;
import media.toloka.rfa.radio.newstoradio.service.NewsService;
import media.toloka.rfa.radio.station.service.StationService;
import media.toloka.rfa.radio.store.Service.StoreService;
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

import java.io.*;
import java.util.*;

import static media.toloka.rfa.radio.model.enumerate.EHistoryType.History_NewsSendToRadio;
import static media.toloka.rfa.radio.model.enumerate.EHistoryType.History_UserSendMailSetPassword;
import static media.toloka.rfa.radio.newstoradio.model.ENewsStatus.NEWS_STATUS_CREATE;
import static media.toloka.rfa.rpc.model.ERPCJobType.JOB_TTS;
import static org.bouncycastle.asn1.iana.IANAObjectIdentifiers.mail;

@Controller
public class NewsHome {

    @Value("${rabbitmq.queueTTS}")
    private String queueTTS;

    @Value("${media.toloka.rfa.station.basename}")
    private String baseSiteAddress;

    @Value("${media.toloka.rfa.server.toradiosever.name}")
    private String toradiosevername;
    @Value("${media.toloka.rfa.server.toradiosever.user}")
    private String toradioseveruser;
    @Value("${media.toloka.rfa.server.toradiosever.psw}")
    private String toradioseverpsw;

//    media.toloka.rfa.server.toradiosever.name=toradio.rfa
//    media.toloka.rfa.server.toradiosever.user=toradio
//    media.toloka.rfa.server.toradiosever.psw=toradio

    @Autowired
    RabbitTemplate template;

    @Autowired
    private GsonService gsonService;


    @Autowired
    private ClientService clientService;

    @Autowired
    private StoreService storeService;

    @Autowired
    private StationService stationService;

    @Autowired
    private NewsService newsService;

    @Autowired
    private HistoryService historyService;

    final Logger logger = LoggerFactory.getLogger(ClientHomeController.class);

    /// видаляємо трек
    @GetMapping(value = "/newstoradio/clearstore/{scurpage}/{uuidnews}")
    public String userDeleteFromStore(
            @PathVariable String uuidnews,
            @PathVariable String scurpage,
            Model model) {

        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(user);
        News news = newsService.GetByUUID(uuidnews);
        if (news == null) {
            return "redirect:/newsradio/home/0";
        }
        List<ENewsCategory> category = Arrays.asList(ENewsCategory.values());
        List<Station> listStation = stationService.GetListStationByCd(cd);
        model.addAttribute("liststation", listStation);
        model.addAttribute("categorys", category);
        model.addAttribute("curnews", news);
        model.addAttribute("currentPage", scurpage);
        // Саме тут видаляємо файл та запис в Сховищі
        Long rc = newsService.deleteNewsTrackFromStore(uuidnews);
        if (rc == 0L) {
            newsService.GetByUUID(uuidnews).setStorespeach(null);
            newsService.GetByUUID(uuidnews).setStatus(NEWS_STATUS_CREATE);
            newsService.Save(newsService.GetByUUID(uuidnews));
            model.addAttribute("success", "Озвучений текст успішно видалено зі сховища");
            return "/newstoradio/editnews";
        }
        model.addAttribute("error", "Озвучений текст не видалено зі сховища");
        return "/newstoradio/viewnews";
    }

    /// видаляємо новину
    @GetMapping(value = "/newstoradio/deletenews/{scurpage}/{uuidnews}")
    public String userDeleteNews(
            @PathVariable String uuidnews,
            @PathVariable String scurpage,
            Model model) {

        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(user);

        News curnews = newsService.GetByUUID(uuidnews);


        if (curnews == null) {

            return "/newstoradio/home/0";
        }

        List<ENewsCategory> category = Arrays.asList(ENewsCategory.values());

        List<Station> listStation = stationService.GetListStationByCd(cd);

        model.addAttribute("liststation", listStation);
        model.addAttribute("categorys", category);
        model.addAttribute("curnews", curnews);
        model.addAttribute("currentPage", scurpage);

        Long rc = newsService.deleteNews(uuidnews);
        if (rc == 0L) model.addAttribute("success", "Новину успішно видалено");
        else model.addAttribute("error", "Новину не видалено");
        return "redirect:/newstoradio/home/0";
    }

    /// Відтворюємо на радіо
    @GetMapping(value = "/newstoradio/newstoradio/{scurpage}/{uuidnews}")
    public String speachToStation(
            @PathVariable String uuidnews,
            @PathVariable String scurpage,
            Model model) {

        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(user);
        Integer curpage = Integer.parseInt(scurpage);

        /// виконуємо трансляцію на радіостанцію
        logger.info("================= виконуємо трансляцію на радіостанцію");
        Long rc = 129L;
        String radioserver;

        if (newsService.GetByUUID(uuidnews).getStation().getRadioserver() != null )
            radioserver = newsService.GetByUUID(uuidnews).getStation().getRadioserver();
        else
            radioserver = newsService.GetByUUID(uuidnews).getStation().getGuiserver();

        String toRadioCommand = "ssh toradio@" + toradiosevername + " ffmpeg -re -v quiet -stats -i https://"+baseSiteAddress+"/store/audio/"
                + newsService.GetByUUID(uuidnews).getStorespeach().getUuid() + " -f mp3 icecast://"
                + toradioseveruser + ":" + toradioseverpsw
                + "@"
                + radioserver + ":"
                + newsService.GetByUUID(uuidnews).getStation().getMain().toString() + "/main &>/dev/null";
        historyService.saveHistory(History_NewsSendToRadio,
                "Новина "+ newsService.GetByUUID(uuidnews)
                        + " станція " + newsService.GetByUUID(uuidnews).getStorespeach().getUuid()
                        +"  команда " + toRadioCommand, user);

        ProcessBuilder pb = new ProcessBuilder("bash", "-c", toRadioCommand);
        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
        } catch (IOException e) {
            logger.warn(" Щось пішло не так при виконанні завдання в операційній системі");
            e.printStackTrace();
        }

        logger.info("================= виконали трансляцію на радіостанцію");
        /// виконуємо трансляцію на радіостанцію

        // повертаємося до поточної сторінки
        return "redirect:/newstoradio/home/" + curpage.toString();
    }

    /// дивимося новину
    @GetMapping(value = "/newstoradio/viewnews/{scurpage}/{uuidnews}")
    public String userCreateJobToTTS(
            @PathVariable String uuidnews,
            @PathVariable String scurpage,
            Model model) {

        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(user);

        News curnews = newsService.GetByUUID(uuidnews);

        List<ENewsCategory> category = Arrays.asList(ENewsCategory.values());

        List<Station> listStation = stationService.GetListStationByCd(cd);

        model.addAttribute("liststation", listStation);
        model.addAttribute("categorys", category);
        model.addAttribute("curnews", curnews);
        model.addAttribute("currentPage", scurpage);

        return "/newstoradio/viewnews";
    }

    /// відправляємо текст на перетворення
    @GetMapping(value = "/newstoradio/ttsprepare/{scurpage}/{uuidnews}")
    public String ttsprepare(
            @PathVariable String uuidnews,
            @PathVariable String scurpage,
            Model model) {

        Users user = clientService.GetCurrentUser();
        Clientdetail clientdetail = clientService.GetClientDetailByUser(user);
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(user);

        News curnews = newsService.GetByUUID(uuidnews);
        if (curnews != null) {
            // знайшли новину. Відправляємо на tts
            NewsRPC rjob = new NewsRPC();
            rjob.setRJobType(JOB_TTS);
            rjob.getFront().setUser(System.getenv("USER"));
            rjob.getFront().setServer(curnews.getStation().getGuiserver());
            rjob.setNewsUUID(curnews.getUuid());
            rjob.setStationUUID(curnews.getStation().getUuid());
            rjob.setText(curnews.getNewsbody());
            rjob.setRc(1024L);

            Gson gson = gsonService.CreateGson();
            template.convertAndSend(queueTTS, gson.toJson(rjob).toString());
            model.addAttribute("success", "Завдання перетворення тексту в голос надіслано на обробку.");
            curnews.setStatus(ENewsStatus.NEWS_STATUS_SEND);
            curnews.setDatechangestatus(new Date());
            newsService.Save(curnews);

        } else {
            model.addAttribute("error", "<b>Щось пішло не так - не знайшли новину "
                    + ".Завдання перетворення тексту в голос не надіслано на обробку.</b>");
            logger.info("==== NEWS ttsprepare: Щось пішло не так - не знайшли новину {}",uuidnews);
        }
        // формуємо інформацію для відображення
        Integer curpage = 0;

// Пейджинг для сторінки
        Page pageStore = newsService.GetNewsPageByClientDetail(curpage, 10, cd);
        List<News> viewList = pageStore.stream().toList();

        model.addAttribute("totalPages", pageStore.getTotalPages());
        model.addAttribute("currentPage", scurpage);
        model.addAttribute("linkPage", "/creater/tracks/");
        model.addAttribute("viewList", viewList);

        return "redirect:/newstoradio/home/" + scurpage;
    }

    /// відображаємо сторінку з новинами
    @GetMapping(value = "/newstoradio/home/{cPage}")
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
        Page pageStore = newsService.GetNewsPageByClientDetail(curpage, 10, cd);
        List<News> viewList = pageStore.stream().toList();
        List<News> newsList = newsService.GetListNewsByCd(cd);
        Boolean runTTS = false;
        for (News runnews : newsList) {
            if (runnews.getStatus() == ENewsStatus.NEWS_STATUS_SEND) {
                runTTS = true;
                model.addAttribute("success", "У черзі на перетворення тексту в голос є завдання. Зараз з новинами нічого не можна робити.");
            }
        }

        model.addAttribute("runstatus", runTTS);
        model.addAttribute("totalPages", pageStore.getTotalPages());
        model.addAttribute("currentPage", curpage);
        model.addAttribute("linkPage", "/creater/tracks/");
        model.addAttribute("viewList", viewList);

        return "/newstoradio/home";
    }

    /// редагуємо новину
    @GetMapping(value = "/newstoradio/editnews/{scurpage}/{uuidnews}")
    public String GetEditNews(
            @PathVariable String uuidnews,
            @PathVariable String scurpage,
            Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(user);

        News curnews = newsService.GetByUUID(uuidnews);
        if (curnews == null) {
            curnews = new News();
            curnews.setClientdetail(cd);
            curnews.setId(0L);
        }
        curnews.setClientdetail(cd);

        List<ENewsCategory> category = Arrays.asList(ENewsCategory.values());

        List<Station> listStation = stationService.GetListStationByCd(cd);

        model.addAttribute("liststation", listStation);
        model.addAttribute("categorys", category);
        model.addAttribute("curnews", curnews);
        model.addAttribute("currentPage", scurpage);

        return "/newstoradio/editnews";
    }

    /// зберігаємо відредаговану новину
    @PostMapping(value = "/newstoradio/editnews/{pagelist}")
    public String newsCreateEditNews(
//            @PathVariable String uuidNews,
            @PathVariable String pagelist,
            @ModelAttribute News fnews,
            Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(user);

//        News news = newsService.GetByUUID(uuidNews);

        News news = null;
        if (fnews.getUuid() != null) {
            news = newsService.GetByUUID(fnews.getUuid());
        }
        Boolean type;
        if (news != null) {
            news.setCategory(fnews.getCategory());
            news.setNewstitle(fnews.getNewstitle());
            news.setNewsbody(fnews.getNewsbody());
            news.setStation(fnews.getStation());
            news.setCategory(fnews.getCategory());
            type = false;
        } else {
            logger.info("Створюємо новину");
            news = new News();
            news.setClientdetail(cd);
            news.setId(System.currentTimeMillis());
            news.setUuid(UUID.randomUUID().toString());
            news.setCategory(fnews.getCategory());
            news.setNewstitle(fnews.getNewstitle());
            news.setNewsbody(fnews.getNewsbody());
            news.setStation(fnews.getStation());
            type = true;
        }

//        logger.info(news.toString());
        if (news != null) newsService.Save(news);

        if (type) return "redirect:/newstoradio/home/0";
        return "redirect:/newstoradio/home/" + pagelist;

    }

}
