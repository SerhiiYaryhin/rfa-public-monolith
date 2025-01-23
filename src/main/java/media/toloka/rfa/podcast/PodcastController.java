package media.toloka.rfa.podcast;


import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.servlet.http.HttpSession;
import media.toloka.rfa.podcast.model.PodcastItem;
import media.toloka.rfa.podcast.service.RSSXMLService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.podcast.model.PodcastChannel;
import media.toloka.rfa.podcast.service.PodcastService;
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

import java.lang.reflect.Array;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.springframework.http.MediaType.TEXT_PLAIN;

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

    final Logger logger = LoggerFactory.getLogger(PodcastController.class);


    @GetMapping(value = "/podcast/home")
    public String podcastroot(
            Model model ) {
        logger.info("Зайшли на /podcast/home");
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) { return "redirect:/"; }

        List<PodcastChannel> podcastslist = podcastService.GetPodcastListByCd(cd);

        model.addAttribute("podcastslist",  podcastslist);
        if (podcastslist.isEmpty()) {
            model.addAttribute("warning", "Ви ще не маєте подкастів. Створіть свій перший подкаст!");
        }

        return "/podcast/home";
    }

    @GetMapping(value = "/podcast/view/{puuid}")
    public String podcastview(
            @PathVariable String puuid,
            Model model ) {
        logger.info("Зайшли на /podcast/view/{}",puuid);


        PodcastChannel podcastChannel = podcastService.GetChanelByUUID(puuid);
        if (podcastChannel == null) {
            logger.info("Хтось помилився посиланням. Знов довбляться на сайт.");
            return "redirect:/";
        }

        model.addAttribute("podcast",  podcastChannel);

        model.addAttribute("ogimage",  podcastChannel.getImage().getStoreidimage().getUuid() );

        return "/podcast/view";
    }

    @PostMapping(value = "/podcast/home")
    public String podcastroot(
            @ModelAttribute Station station,
            @ModelAttribute Users formUserPSW,
            Model model ) {

            // Users user = clientService.GetCurrentUser();

            // TODO відправити повідомлення на сторінку
            model.addAttribute("success",  "Реакція на POST зі сторінки /podcast/proot");
        return "redirect:/podcast/home";
    }

    /*
    Сторінка з переліком наявних на порталі подкастів з пагінацією

 */
    @GetMapping(value = "/podcast/all")
    public String podcastAllview(
//            @PathVariable String puuid,
            Model model ) {

        List<PodcastChannel> podcastChList = podcastService.GetAllChanel();
        model.addAttribute("podcastList",  podcastChList);

        return "/guest/podcastall";
    }

    // формуємо RSS для конкретного подкасту.
    @GetMapping(value = "/podcast/rss/{puuid}")
    public ResponseEntity<byte[]> podcastRss(
            @PathVariable String puuid,
            Model model ) {

//        logger.info("Get RSS for RFA podcast {}",puuid);
        PodcastChannel pc = podcastService.GetChanelByUUID(puuid);
        // хтось довбиться за назвою епізодів
        // 66.249.66.167 => http://rfa.toloka.media/podcast/rss/%D0%9B%D0%98%D0%A1%D0%98%D0%A6%D0%AF%20%D0%86%20%D0%A0%D0%90%D0%9A
        // ЛИСИЦЯ І РАК
        String decodedUrl;
        if (pc == null ) {
            decodedUrl = URLDecoder.decode(puuid, StandardCharsets.UTF_8);
            System.out.println(decodedUrl);
//            logger.info("========== хтось довбиться за назвою епізоду");
            List<PodcastItem> podcastItemList =  podcastService.GetListByTitle(decodedUrl);
            if (podcastItemList.size() != 1) {
                String is = "ERROR";
                byte[] byteArray = is.getBytes();
                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-type", MediaType.TEXT_XML_VALUE);
                return ResponseEntity.status(HttpStatus.OK).headers(headers).body(byteArray);
            } else {
                pc = podcastItemList.get(0).getChanel();
                logger.info("========== хтось довбиться за назвою епізоду: "+decodedUrl);
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
            Model model ) {

        PodcastItem podcastItem = podcastService.GetEpisodeByUUID(euuid);
        model.addAttribute("podcastItem",  podcastItem);

        return "/podcast/episode";
    }

    /* Працюємо із завантаженням подкасту з RSS URL */

    /* забираємо подкаст за посиланням на RSS */
    @GetMapping(value = "/podcast/GetRSSFromUrl")
    public String GetRssFromUrl(            @PathVariable String euuid,
                                     Model model ) {

        return "/podcast/GetRSSFromUrl";
    }

}
