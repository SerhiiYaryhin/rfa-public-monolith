package media.toloka.rfa.podcast;


import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.servlet.http.HttpSession;
import lombok.Getter;
import lombok.Setter;
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
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;

//import static org.jcp.xml.dsig.internal.dom.DOMUtils.getAttributeValue;
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

    // клас для форми urla
    @Getter
    @Setter
    public class strUrl {
        String RSSFromUrl = "";
    }

    public strUrl tmpstrUrl = new strUrl();

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
            logger.info("Хтось помилився посиланням. Знов довбляться на сайт.");
            return "redirect:/";
        }

        model.addAttribute("podcast", podcastChannel);

        model.addAttribute("ogimage", podcastChannel.getImage().getStoreidimage().getUuid());

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
        model.addAttribute("podcastItem", podcastItem);

        return "/podcast/episode";
    }

    /* Працюємо із завантаженням подкасту з RSS URL */


    /* забираємо подкаст за посиланням на RSS */
    @GetMapping(value = "/podcast/getRSSFromUrl")
    public String GetPodcastFromRSSUrl(            //@PathVariable String euuid,
                                                   Model model) {
        // https://anchor.fm/s/89f5c40c/podcast/rss
        model.addAttribute("strUrl", tmpstrUrl);

        return "/podcast/getRSSFromUrl";
    }

    @PostMapping(value = "/podcast/getRSSFromUrl")
    public String PostPodcastFromRSSUrl(
            @ModelAttribute strUrl gstrUrl,
            Model model) {
        tmpstrUrl.setRSSFromUrl(gstrUrl.getRSSFromUrl());
        logger.info("===== {}", tmpstrUrl.RSSFromUrl);
        String rssContent = fetchRssContent(tmpstrUrl.RSSFromUrl);
        // Парсинг RSS в DOM
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        Document doc = null;
        try {
            builder = factory.newDocumentBuilder();
            doc = builder.parse(new InputSource(new StringReader(rssContent)));
        } catch (ParserConfigurationException e) {
            logger.info("ParserConfigurationException: Помилка перетворення на XML");
            return null;
        } catch (SAXException e) {
            logger.info("SAXException: Помилка перетворення на XML");
            return null;
        } catch (IOException e) {
            logger.info("IOException: Помилка перетворення на XML");
            return null;
        }

        NodeList items = doc.getElementsByTagName("item");
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);

            String title = getElementValue(item, "title");
            logger.info("Title {} : {}",i,title);
            String link = getElementValue(item, "link");
            logger.info("Link:{}",link);
            String audioUrl = getAttributeValue(item, "enclosure", "url"); // Посилання на аудіофайл
            logger.info("audioUrl:{}",audioUrl);
//            LocalDateTime pubDate = parsePubDate(getElementValue(item, "pubDate"));

            // Перевірка чи епізод вже є в базі
//            if (episodeRepository.findByTitle(title).isEmpty()) {
//                Episode episode = new Episode();
//                episode.setTitle(title);
//                episode.setLink(link);
//                episode.setPublicationDate(pubDate);
//
//                episodeRepository.save(episode);
//                System.out.println("Додано новий епізод: " + title);
//            }
        }
//    }
//    catch (Exception e) {
//        e.printStackTrace();
//    }


//        model.addAttribute("gstrUrl",  gstrUrl);
        return "redirect:/podcast/getRSSFromUrl";
    }

    private String fetchRssContent(String rssUrl) {
        URL url = null;
        HttpURLConnection connection;
        try {
            url = new URL(rssUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
        } catch (MalformedURLException e) {
            logger.info("MalformedURLException: Помилка перетворення на URL");
            return null;
        } catch (IOException e) {
            logger.info("IOException: Помилка при читанні URL");
            return null;
        }

        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream()))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        } catch (IOException e) {
            logger.info("Помилка при читанні потоку");
            return null;
        }
    }

    private String getElementValue(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }
    private String getAttributeValue(Element parent, String tagName, String attributeName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Element element = (Element) nodeList.item(0);
            return element.getAttribute(attributeName); // Повертаємо значення атрибута
        }
        return null;
    }


}
