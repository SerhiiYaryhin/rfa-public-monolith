package media.toloka.rfa.radio.newstoradio;


import com.google.gson.Gson;
import media.toloka.rfa.config.gson.service.GsonService;
import media.toloka.rfa.radio.client.ClientHomeController;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.radio.newstoradio.model.ENewsCategory;
import media.toloka.rfa.radio.newstoradio.model.ENewsStatus;
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

import static media.toloka.rfa.radio.model.enumerate.EPostStatus.POSTSTATUS_REDY;
import static media.toloka.rfa.rpc.model.ERPCJobType.JOB_TTS;

@Controller
public class home {

    @Value("${rabbitmq.queueTTS}")
    private String queueTTS;

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

    final Logger logger = LoggerFactory.getLogger(ClientHomeController.class);

    @GetMapping(value = "/newstoradio/ttsprepare/{uuidnews}")
    public String userCreateJobToTTS(
            @PathVariable String uuidnews,
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
            logger.info("==== home userCreateJobToTTS Front USER {} {}",System.getenv("USER"),rjob.getTts().getUser());
            rjob.getFront().setServer(System.getenv("HOSTNAME"));
            logger.info("==== home userCreateJobToTTS Front HOSTNAME {} {}",System.getenv("HOSTNAME"),rjob.getTts().getServer());
            rjob.setNewsUUID(curnews.getUuid());
            rjob.setRc(1024L);

            Gson gson = gsonService.CreateGson();
            template.convertAndSend(queueTTS, gson.toJson(rjob).toString());
            model.addAttribute("success", "Завдання перетворення тексту в голос надіслано на обробку.");
            curnews.setStatus(ENewsStatus.NEWS_STATUS_SEND);
            curnews.setDatechangestatus(new Date());
            newsService.Save(curnews);

        } else {
            model.addAttribute("error", "<b>Щось пішло не так.<br>Завдання перетворення тексту в голос не надіслано на обробку.</b>");
        }
        // формуємо інформацію для відображення
        Integer curpage = 0;

// Пейджинг для сторінки
        Page pageStore = newsService.GetNewsPageByClientDetail(curpage, 10, cd);
        List<News> viewList = pageStore.stream().toList();
//        List<News> tl = newsService.GetListNewsByCd(cd);

        model.addAttribute("totalPages", pageStore.getTotalPages());
        model.addAttribute("currentPage", curpage);
        model.addAttribute("linkPage", "/creater/tracks/");
        model.addAttribute("viewList", viewList);

        return "redirect:/newstoradio/home/0";
    }

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
//        List<News> tl = newsService.GetListNewsByCd(cd);

        model.addAttribute("totalPages", pageStore.getTotalPages());
        model.addAttribute("currentPage", curpage);
        model.addAttribute("linkPage", "/creater/tracks/");

        model.addAttribute("viewList", viewList);

        return "/newstoradio/home";
    }


    @GetMapping(value = "/newstoradio/editnews/{uuidnews}")
    public String GetEditNews(
            @PathVariable String uuidnews,
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

        return "/newstoradio/editnews";
    }

    @PostMapping(value = "/newstoradio/editnews")
    public String newsCreateEditNews(
//            @PathVariable String uuidNews,
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
        if (news != null) {
            news.setCategory(fnews.getCategory());
            news.setNewstitle(fnews.getNewstitle());
            news.setNewsbody(fnews.getNewsbody());
            news.setStation(fnews.getStation());
            news.setCategory(fnews.getCategory());
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
        }

        logger.info(news.toString());
        if (news != null) newsService.Save(news);

        return "redirect:/newstoradio/home/0";

    }


}
