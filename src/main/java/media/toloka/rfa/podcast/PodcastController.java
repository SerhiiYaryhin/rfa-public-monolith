package media.toloka.rfa.podcast;


import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import media.toloka.rfa.podcast.model.PodcastItem;
import media.toloka.rfa.podcast.service.RSSXMLService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.podcast.model.PodcastChannel;
import media.toloka.rfa.podcast.service.PodcastService;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.security.model.Users;
import org.hibernate.SessionFactory;
import org.hibernate.internal.SessionFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.module.Configuration;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;

@Profile("Front")
@Controller
public class PodcastController {
// стандарт RSS для подкаста
// https://podcast-standard.org/podcast_standard/


//    @PersistenceContext
//    EntityManager entityManager;

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

//    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_USER')")
    @GetMapping(value = {"/podcast/home", "/podcast/home/{page}"})
    public String podcastroot(
            @PathVariable(required = false) Integer page,
            Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(user);
        if (cd == null) {
            return "redirect:/";
        }

        int currentPage = (page == null) ? 0 : page;
        Page<PodcastChannel> podcastsPage = podcastService.GetPodcastPageByCd(cd, currentPage, 12);

        model.addAttribute("podcastslist", podcastsPage.getContent());
        model.addAttribute("totalPages", podcastsPage.getTotalPages());
        model.addAttribute("currentPage", currentPage);

        if (podcastsPage.isEmpty()) {
            model.addAttribute("warning", "Ви ще не маєте подкастів. Створіть свій перший подкаст!");
        }

        return "/podcast/home";
    }

    @GetMapping(value = {"/podcast/view/{puuid}", "/podcast/view/{puuid}/{page}"})
    public String podcastview(
            @PathVariable String puuid,
            @PathVariable(required = false) Integer page,
            Model model) {
        int currentPage = (page == null) ? 0 : page;
        logger.info("Зайшли на /podcast/view/{} сторінка {}", puuid, currentPage);
        
        PodcastChannel podcastChannel = podcastService.GetChanelByUUID(puuid);
        if (podcastChannel == null) {
            List<PodcastChannel> podcastChannelList = podcastService.GetChanelByTitle(puuid);
            if (podcastChannelList != null && podcastChannelList.size() == 1) {
                podcastChannel = podcastChannelList.get(0);
            }
        }

        if (podcastChannel == null) {
            model.addAttribute("warning", "Такого подкасту не існує.");
            return "redirect:/podcast/all";
        }

        // Отримуємо пагіновані епізоди
        Page<PodcastItem> episodesPage = podcastService.GetEpisodesByChanelPage(podcastChannel, currentPage, 12);
        
        podcastChannel.setLooked(podcastChannel.getLooked() + 1L);
        podcastService.SavePodcast(podcastChannel);

        model.addAttribute("podcast", podcastChannel);
        model.addAttribute("episodes", episodesPage.getContent());
        model.addAttribute("totalPages", episodesPage.getTotalPages());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("ogimage", podcastChannel.getImagechanelstore() != null ? podcastChannel.getImagechanelstore().getUuid() : "");

        return "/podcast/view";
    }


//    @Secured({ "ROLE_ADMIN","ROLE_MODERATOR"})
//    @Secured({"ROLE_USER", "ROLE_CREATOR", "ROLE_ADMIN","ROLE_MODERATOR"})
    @PostMapping(value = "/podcast/home")
    public String podcastroot(
            @ModelAttribute Station station,
            @ModelAttribute Users formUserPSW,
            Model model) {

         Users user = clientService.GetCurrentUser();

        // TODO відправити повідомлення на сторінку
        model.addAttribute("success", "Реакція на POST зі сторінки /podcast/proot");
        return "/podcast/home";
    }

    /**
     * Відображаємо Сторінку з переліком наявних та дозволених подкастів на порталі з пагінацією
     *
     * @param model
     * @return
     */
    @GetMapping(value = {"/podcast/all", "/podcast/all/{page}"})
    public String podcastAllview(
            @PathVariable(required = false) Integer page,
            Model model) {
        int currentPage = (page == null) ? 0 : page;
        Page<PodcastChannel> podcastPage = podcastService.GetAllApprovedChanelsPage(currentPage, 12);
        
        model.addAttribute("podcastList", podcastPage.getContent());
        model.addAttribute("totalPages", podcastPage.getTotalPages());
        model.addAttribute("currentPage", currentPage);

        return "/guest/podcastall";
    }

    /**
     * формуємо RSS feed для конкретного подкасту.
     *
     * @param puuid UUID подкасту
     * @param model
     * @return String XML RSS FEED
     */
    //
    @GetMapping(value = "/podcast/rss/{puuid}")
    public ResponseEntity<byte[]> podcastRss(
            @PathVariable String puuid,
            Model model) {

        PodcastChannel pc = podcastService.GetChanelByUUID(puuid);

        String decodedUrl;
        if (pc == null) {
            decodedUrl = URLDecoder.decode(puuid, StandardCharsets.UTF_8);
            System.out.println(decodedUrl);
            List<PodcastItem> podcastItemList = podcastService.GetListByTitle(decodedUrl);
            if (podcastItemList.size() != 1) {
                String is = "ERROR";
                byte[] byteArray = is.getBytes();
                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-type", MediaType.TEXT_XML_VALUE);
                return ResponseEntity.status(HttpStatus.OK).headers(headers).body(byteArray);
            } else {
                pc = podcastItemList.get(0).getChanel();
                logger.info("\n========== хтось довбиться за назвою епізоду:\n" + decodedUrl);
            }
        }
        // саме тут ми формуємо RSS feed
        byte[] byteArray = rssxmlService.MakeRSSXMLService(pc).getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-type", MediaType.TEXT_XML_VALUE);

        return ResponseEntity.status(HttpStatus.OK).headers(headers).body(byteArray);
    }

    /**
     * Відображаємо епізод подкасту
     *
     * @param euuid UUID  епізоду
     * @param model
     * @return /podcast/episode
     */
    // відображаємо епізод подкасту
    @GetMapping(value = "/podcast/episode/{euuid}")
    public String podcastEpisodeView(
            @PathVariable String euuid,
            Model model) {

        PodcastItem podcastItem = podcastService.GetEpisodeByUUID(euuid);
        if (podcastItem == null) {
            podcastItem = podcastService.GetEpisodeByTitle(euuid);
        }
        if (podcastItem != null) {
            // рахуємо заходи на епізод
            podcastItem.setLooked(podcastItem.getLooked()+1L);
            podcastService.SaveEpisode(podcastItem);

            model.addAttribute("podcastItem", podcastItem);
            return "/podcast/episode";
        } else {
            model.addAttribute("danger", "Щось пішло не так. Такий епізод не існує або, його переміщено .");
            logger.warn("Отримали UUID епізоду, який не існує: {}", euuid);
        }
        return "/podcast/episode";
    }

    /** Список подкастів поточного автора з пагінацією */
    @GetMapping(value = "/creater/podcasts")
    public String getCreatorPodcasts(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";
        
        Clientdetail cd = clientService.GetClientDetailByUser(user);
        int currentPage = page;
        
        // Отримуємо пагіновані подкасти конкретного автора
        Page<PodcastChannel> podcastPage = podcastService.GetPodcastsByClientPage(cd.getUuid(), currentPage, 10);

        model.addAttribute("podcastList", podcastPage.getContent());
        model.addAttribute("totalPages", podcastPage.getTotalPages());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("linkPage", "/creater/podcasts");

        return "/creater/podcasts";
    }

    @GetMapping(value = "/creater/podcasts/{page}")
    public String getCreatorPodcastsOld(@PathVariable Integer page) {
        return "redirect:/creater/podcasts?page=" + page;
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


//        EntityManagerFactoryInfo info = (EntityManagerFactoryInfo) entityManager.getEntityManagerFactory();
//        try {
//            String db = info.getDataSource().getConnection().getMetaData().getURL();
//            db = db.substring( db.lastIndexOf("/")+1);
//            logger.info("++++++ Поточна база даних: {}" ,db.toUpperCase());
//        } catch (SQLException e) {
//            logger.info("Щось пішло не так при визначенні імені бази даних :(");
//        }


        // https://anchor.fm/s/89f5c40c/podcast/rss  Казки Суспільне
        // https://anchor.fm/s/ff57ac9c/podcast/rss

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

    /**
     * Видаляємо подкаст
     *
     * @param model
     * @return тимчасову сторінку яка повинна бути доступна тільки модераторам.
     * @call podcastService.PutPodcastFromRSS(model, gstrUrl) // саме тут забираємо подкаст
     */    // Виводимо поле з посиланням та результат обробки завантаженого RSS.
    @GetMapping(value = "/podcast/pdel/{puuid}")
    public String PostPodcastFromRSSUrl(
            @PathVariable String puuid,
//            @ModelAttribute PodcastService.strUrl gstrUrl,
            Model model) {

        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) {
            return "redirect:/";
        }
        PodcastChannel pc = podcastService.GetChanelByUUID(puuid);
        if (pc != null) {
            if (pc.getClientdetail().equals(cd.getUuid())) {
                PodcastService.strUrl gstrUrl; // = new PodcastService.strUrl();
                gstrUrl = podcastService.GetNewStrurl();
                logger.info("Видаляємо подкаст імпортований з: {}", pc.getLinktoimporturl());
                gstrUrl.setRSSFromUrl(pc.getLinktoimporturl());
                gstrUrl.setClrpodcast(true);
                podcastService.PutPodcastFromRSS(model, gstrUrl);
            } else {
                logger.info("Намагаємося видалити не свій подкаст");
            }
        } else {
            logger.info("Подкаст, який намагаємося видалити, не знайдено!");
        }

        // Саме тут видаляємо подкаст

        return "redirect:/podcast/home";

    }

    /**
     * Видаляємо подкаст
     *
     * @param model
     * @return тимчасову сторінку яка повинна бути доступна тільки модераторам.
     * @call podcastService.PutPodcastFromRSS(model, gstrUrl) // саме тут забираємо подкаст
     */    // Виводимо поле з посиланням та результат обробки завантаженого RSS.
    @GetMapping(value = "/podcast/localpodcastdel/{puuid}")
    public String PostPodcastDeleteFromRFA(
            @PathVariable String puuid,
//            @ModelAttribute PodcastService.strUrl gstrUrl,
            Model model) {

        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) {
            return "redirect:/";
        }
        PodcastChannel pc = podcastService.GetChanelByUUID(puuid);
        if (pc != null) {
            if (pc.getClientdetail().equals(cd.getUuid())) { // якщо подкаст належить цьому користувачу, то чистимо його
                if (!podcastService.ClearAndDeletePodcastChanel(pc)) {
                    model.addAttribute("danger", "Подкаст не Видалено.");
                }
            } else {
                logger.info("Намагаємося видалити не свій подкаст");
            }
        } else {
            logger.info("Подкаст, який намагаємося видалити, не знайдено!");
        }

        // Саме тут видаляємо подкаст

        return "redirect:/podcast/home";

    }


}
