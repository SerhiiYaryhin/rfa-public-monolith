package media.toloka.rfa.podcast;


import media.toloka.rfa.podcast.model.PodcastItem;
import media.toloka.rfa.podcast.service.RSSXMLService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.podcast.model.PodcastChannel;
import media.toloka.rfa.podcast.service.PodcastService;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Controller
public class PodcastController {
// стандарт RSS для подкаста
// https://podcast-standard.org/podcast_standard/


    @Autowired
    private PodcastService podcastService;
    @Autowired
    private RSSXMLService rssxmlService;
    @Autowired
    private ClientService clientService;
    @Autowired
    private StoreService storeService;

//    // клас для форми urla
//    @Getter
//    @Setter
//    public class strUrl {
//        String RSSFromUrl = "";
//        PodcastChannel podcastChannel = null;// = new PodcastChannel();
//        Boolean fill = false;
//        Boolean tested = true;
//        Boolean clrpodcast = false;
//    }

//    public strUrl tmpstrUrl = new strUrl();

    final Logger logger = LoggerFactory.getLogger(PodcastController.class);


    @GetMapping(value = "/podcast/home")
    public String podcastroot(
            Model model) {
        logger.info("Зайшли на /podcast/home");
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) {
            return "redirect:/";
        }

        List<PodcastChannel> podcastslist = podcastService.GetPodcastListByCd(cd);

        model.addAttribute("podcastslist", podcastslist);
        if (podcastslist.isEmpty()) {
            model.addAttribute("warning", "Ви ще не маєте подкастів. Створіть свій перший подкаст!");
        }

        return "/podcast/home";
    }

    @GetMapping(value = "/podcast/view/{puuid}")
    public String podcastview(
            @PathVariable String puuid,
            Model model) {
        logger.info("Зайшли на /podcast/view/{}", puuid);
        PodcastChannel podcastChannel = podcastService.GetChanelByUUID(puuid);
        if (podcastChannel == null) {
            logger.info("Хтось помилився посиланням на подкаст та/або знов довбляться на сайт.");
            model.addAttribute("warning", "Такого подкасту не існує. Ви або помилилися посиланням, або він переміщений.");
        } else {
            if (podcastChannel.getImagechanelstore() != null) {
                model.addAttribute("ogimage", podcastChannel.getImagechanelstore().getUuid());
            } else {
                // todo Вставити нормальне посилання на cover за замовчуванням
                model.addAttribute("ogimage", "------------");
            }
        }
        model.addAttribute("podcast", podcastChannel);
        return "/podcast/view";
    }

    @PostMapping(value = "/podcast/home")
    public String podcastroot(
            @ModelAttribute Station station,
            @ModelAttribute Users formUserPSW,
            Model model) {

        // Users user = clientService.GetCurrentUser();

        // TODO відправити повідомлення на сторінку
        model.addAttribute("success", "Реакція на POST зі сторінки /podcast/proot");
        return "redirect:/podcast/home";
    }

    /*
    Сторінка з переліком наявних на порталі подкастів з пагінацією

 */
    @GetMapping(value = "/podcast/all")
    public String podcastAllview(
//            @PathVariable String puuid,
            Model model) {

        List<PodcastChannel> podcastChList = podcastService.GetAllChanel();
        model.addAttribute("podcastList", podcastChList);

        return "/guest/podcastall";
    }

    // формуємо RSS для конкретного подкасту.
    @GetMapping(value = "/podcast/rss/{puuid}")
    public ResponseEntity<byte[]> podcastRss(
            @PathVariable String puuid,
            Model model) {

//        logger.info("Get RSS for RFA podcast {}",puuid);
        PodcastChannel pc = podcastService.GetChanelByUUID(puuid);
        // хтось довбиться за назвою епізодів
        // 66.249.66.167 => http://rfa.toloka.media/podcast/rss/%D0%9B%D0%98%D0%A1%D0%98%D0%A6%D0%AF%20%D0%86%20%D0%A0%D0%90%D0%9A
        // ЛИСИЦЯ І РАК
        String decodedUrl;
        if (pc == null) {
            decodedUrl = URLDecoder.decode(puuid, StandardCharsets.UTF_8);
            System.out.println(decodedUrl);
//            logger.info("========== хтось довбиться за назвою епізоду");
            List<PodcastItem> podcastItemList = podcastService.GetListByTitle(decodedUrl);
            if (podcastItemList.size() != 1) {
                String is = "ERROR";
                byte[] byteArray = is.getBytes();
                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-type", MediaType.TEXT_XML_VALUE);
                return ResponseEntity.status(HttpStatus.OK).headers(headers).body(byteArray);
            } else {
                pc = podcastItemList.get(0).getChanel();
                logger.info("========== хтось довбиться за назвою епізоду: " + decodedUrl);
            }
        }
        byte[] byteArray = rssxmlService.MakeRSSXMLService(pc).getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-type", MediaType.TEXT_XML_VALUE);

        return ResponseEntity.status(HttpStatus.OK).headers(headers).body(byteArray);
    }

    // відображаємо епізод подкасту
    @GetMapping(value = "/podcast/episode/{euuid}")
    public String podcastEpisodeView(
            @PathVariable String euuid,
            Model model) {

        PodcastItem podcastItem = podcastService.GetEpisodeByUUID(euuid);
        if (podcastItem != null) {
            model.addAttribute("podcastItem", null);
            return "/podcast/episode";
        } else {
            model.addAttribute("danger", "Щось пішло не так. Такий епізод не знайдено.");
        }
        return "/podcast/episode";
    }

    /* Працюємо із завантаженням подкасту з RSS URL */


    /**
     * забираємо подкаст за посиланням на RSS
     *
     * @param gstrUrl - посилання на RSS, флаг видалення подкасту за RSS та флаг для тестування (поки не використовую)
     * @param model
     * @return тимчасову сторінку яка повинна бути доступна тільки модераторам.
     */
    @GetMapping(value = "/podcast/getRSSFromUrl")
    public String GetPodcastFromRSSUrl(            //@PathVariable String euuid,
                                                   @ModelAttribute PodcastService.strUrl gstrUrl,
                                                   Model model) {
//        strUrl tmpstrUrl = new strUrl();
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) {
            return "redirect:/";
        }

        // https://anchor.fm/s/89f5c40c/podcast/rss  Казки Суспільне
        // https://anchor.fm/s/ff57ac9c/podcast/rss


//        List<Users> usersList = clientService.GetAllUsers();
//        for (Users usr : usersList) {
//            if (usr.getClientdetail() != null) {
//                logger.info("===== User: {} {} {}", usr.getEmail(), usr.getClientdetail().getCustname(), usr.getClientdetail().getCustsurname());
//                List<Roles> rolesList = usr.getRoles();
//                for (Roles rls : rolesList) {
//                    logger.info("=========   Role: {} - {}", rls.getId(), rls.getRole().label);
//                }
//
//            }
//        }

        model.addAttribute("strUrl", gstrUrl);
        return "/podcast/getRSSFromUrl";
    }


    /**
     * Завантажуємо подкаст за посиланням на RSS
     *
     * @param gstrUrl - посилання на RSS, флаг видалення подкасту за RSS та флаг для тестування (поки не використовую)
     * @param model
     * @return тимчасову сторінку яка повинна бути доступна тільки модераторам.
     * @call podcastService.PutPodcastFromRSS(model, gstrUrl) // саме тут забираємо подкаст
     */    // Виводимо поле з посиланням та результат обробки завантаженого RSS.
    @PostMapping(value = "/podcast/getRSSFromUrl")
    public String PostPodcastFromRSSUrl(
            @ModelAttribute PodcastService.strUrl gstrUrl,
            Model model) {

        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) {
            return "redirect:/";
        }

        // перевіряємо довжину посилання.
        if (gstrUrl.getRSSFromUrl().length() < 5) {
            model.addAttribute("warning", "Заповніть поле адреси.");
            return "/podcast/getRSSFromUrl";
        }

        // Саме тут завантажуємо подкаст
        podcastService.PutPodcastFromRSS(model, gstrUrl);

        return "/podcast/getRSSFromUrl";

    }


}
