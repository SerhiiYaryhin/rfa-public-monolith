package media.toloka.rfa.podcast;


import lombok.Getter;
import lombok.Setter;
import media.toloka.rfa.podcast.model.PodcastItem;
import media.toloka.rfa.podcast.service.RSSXMLService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.podcast.model.PodcastChannel;
import media.toloka.rfa.podcast.service.PodcastService;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.security.model.Roles;
import media.toloka.rfa.security.model.Users;
import media.toloka.rfa.service.DownloadFileException;
import media.toloka.rfa.service.DownloadFileResult;
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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static media.toloka.rfa.radio.store.model.EStoreFileType.STORE_EPISODETRACK;
import static media.toloka.rfa.radio.store.model.EStoreFileType.STORE_PODCASTCOVER;
import static media.toloka.rfa.service.FileDownloader.downloadFile;

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

    // клас для форми urla
    @Getter
    @Setter
    public class strUrl {
        String RSSFromUrl = "";
        PodcastChannel podcastChannel = null;// = new PodcastChannel();
        Boolean fill = false;
        Boolean tested = true;
        Boolean clrpodcast = false;
    }

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
            logger.info("Хтось помилився посиланням. Знов довбляться на сайт.");
            return "redirect:/";
        }

        model.addAttribute("podcast", podcastChannel);

        if (podcastChannel.getImage() != null){
            model.addAttribute("ogimage", podcastChannel.getImage().getStoreidimage().getUuid());
        } else {
            // todo Вставити нормальне посилання на cover за замовчуванням
            model.addAttribute("ogimage", "------------");
        }

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
        strUrl tmpstrUrl = new strUrl();
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

        model.addAttribute("strUrl", tmpstrUrl);
        return "/podcast/getRSSFromUrl";
    }


    // Виводимо поле з посиланням та результат обробки завантаженого RSS.
    @PostMapping(value = "/podcast/getRSSFromUrl")
    public String PostPodcastFromRSSUrl(
            @ModelAttribute strUrl gstrUrl,
            Model model) {

        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) {
            return "redirect:/";
        }

        if (gstrUrl.getRSSFromUrl().length() < 5) {
            model.addAttribute("warning", "Заповніть поле адреси.");

            return "/podcast/getRSSFromUrl";
        }

        // Створили пусту структуру
        gstrUrl.setTested(false);
        if (gstrUrl.getTested()) {
            PodcastChannel mypc = podcastService.GetChanelByUUID("402f655b-addb-4faa-952d-e444952a8dc7");
            List<PodcastItem> mypodcastItem = mypc.getItem();
            PodcastItem mypi;
            mypi = podcastService.GetEpisodeByUUID("bde7528c-f1d0-4860-9485-5b7b886e8a2b");
            mypodcastItem.add(mypi);
            mypi = podcastService.GetEpisodeByUUID("8fcd627f-30d8-43a8-8001-8051e941a8ab");
            mypodcastItem.add(mypi);
            podcastService.SavePodcast(mypc);

            model.addAttribute("strUrl", gstrUrl);
            return "/podcast/getRSSFromUrl";
        }

        strUrl tmpstrUrl = new strUrl();

        tmpstrUrl.setRSSFromUrl(gstrUrl.getRSSFromUrl());
        tmpstrUrl.setClrpodcast(gstrUrl.getClrpodcast());
        logger.info("===== {}", tmpstrUrl.RSSFromUrl);

        String rssContent;
        rssContent = fetchRssContent(tmpstrUrl.RSSFromUrl);
//        tmpstrUrl.setPodcastChannel(null);
        // Парсинг RSS в DOM
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        Document doc = null;
        try {
            builder = factory.newDocumentBuilder();
            doc = builder.parse(new InputSource(new StringReader(rssContent)));
            doc.getDocumentElement().normalize();
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

        // Зчитуємо всі атрибути подкасту
        // Отримуємо елементи <channel>
        org.w3c.dom.Node channelNode = doc.getElementsByTagName("channel").item(0);
        org.w3c.dom.Element channelElement = (org.w3c.dom.Element) channelNode;

        // todo тут буде губитися памʼять бо можливо у нас вже був створений подкаст, а ми його просто затерли.
        //  Для етапу розробки - нормально

        List<PodcastChannel> podcastChannelList;
        String podcastTitle;

        // отримали заголовок контенту з RSS
        podcastTitle = channelElement.getElementsByTagName("title").item(0).getTextContent();
        logger.info("Взяли з RSS podcastTitle:{}", podcastTitle);
        // шукаємо в базі подкастів з такою назвою
        podcastChannelList = podcastService.GetChanelByTitle(podcastTitle);

        // Чи видаляємо подкаст?
        if (gstrUrl.getClrpodcast()) {
            /* Delete podcast */
            // шукаємо в базі подкастів подкаст з такою назвою
//            podcastChannelList = podcastService.GetChanelByTitle(podcastTitle);
            if (podcastChannelList != null) {
                for (PodcastChannel lChannel : podcastChannelList) { // пробігаємося по отриманих подкастах з такою назвою
                    if (cd == lChannel.getClientdetail()) { // якщо подкаст належить цьому користувачу, то чистимо його
                        podcastService.ClearAndDeletePodcastChanel(lChannel);
                    }
                }
            } else {
                model.addAttribute("warning", "Такий подкаст не знайдено.");
            }
            model.addAttribute("strUrl", gstrUrl);

            return "/podcast/getRSSFromUrl";
        }

        // дивимося, чи пустий подкаст
        // Навіщо ми це дивимося?
        // у цій процедурі створили tmpstrUrl і тому не має потреби нічого перевіряти
//        if (tmpstrUrl.getPodcastChannel() != null) {
//            /* Подкаст не пустий. Чистимо поточний перелік епізодів подкасту */
//            // Навіщо ми це чистимо? Може варто оновити?
//            tmpstrUrl.getPodcastChannel().getItem().clear();
//        } else {
//            // подкаст ще пустий. Створюємо подкаст
//            tmpstrUrl.setPodcastChannel(new PodcastChannel());
//        }

        // раніше вже взяли актуальний з RSS
//        podcastTitle = channelElement.getElementsByTagName("title").item(0).getTextContent();

        tmpstrUrl.setPodcastChannel(new PodcastChannel());
        // шукаємо в базі подкастів з такою назвою
        podcastChannelList = podcastService.GetChanelByTitle(podcastTitle);
        Boolean updatePodcast = false;
        if (podcastChannelList != null) {
            updatePodcast = true;
            if (podcastChannelList.size() == 1) {
                logger.info("===== Подкаст з такою назвою вже існує!!!");
                tmpstrUrl.setPodcastChannel(podcastChannelList.get(0));
            } else {
                logger.info("Йой! Подкастів з такою назвою декілька!!!");
                for (PodcastChannel pc : podcastChannelList) {

                    logger.info("==== {} \n    url:{}", pc.getTitle(), pc.getLinktoimporturl());
                }
            }
        }

        // тимчасово. Завантажуємо інформацію та cover подкасту
        updatePodcast = false;
        if (!updatePodcast) {

            String podcastDescription = channelElement.getElementsByTagName("description").item(0).getTextContent();
            logger.info("podcastDescription:{}", podcastDescription);

            String podcastLink = channelElement.getElementsByTagName("link").item(0).getTextContent();
            logger.info("podcastLink:{}", podcastLink);
            String podcastLanguage = channelElement.getElementsByTagName("language").item(0).getTextContent();
            logger.info("podcastLanguage:{}", podcastLanguage);
            String podcastCopyright = channelElement.getElementsByTagName("copyright").item(0) != null ?
                    channelElement.getElementsByTagName("copyright").item(0).getTextContent() : "";
            // витягуємо картинку подкасту
            /* Створили подкаст та заповнили необхідні атрибути подкасту */

            tmpstrUrl.getPodcastChannel().setDescription(podcastDescription);
            tmpstrUrl.getPodcastChannel().setTitle(podcastTitle);
            tmpstrUrl.getPodcastChannel().setLastbuilddate(new Date());
            tmpstrUrl.getPodcastChannel().setClientdetail(cd);
            tmpstrUrl.getPodcastChannel().setLinktoimporturl(tmpstrUrl.RSSFromUrl);
            tmpstrUrl.getPodcastChannel().setLanguage(podcastLanguage);
            tmpstrUrl.getPodcastChannel().setCopyright(podcastCopyright);
            /* зберегли подкаст без епізодів */
            podcastService.SavePodcast(tmpstrUrl.getPodcastChannel());
            // зберігаємо обкладинку
            org.w3c.dom.Node imgNode = doc.getElementsByTagName("image").item(0);
            org.w3c.dom.Element imgElement = (org.w3c.dom.Element) imgNode;
            String imgUrl = imgElement.getElementsByTagName("url").item(0).getTextContent();

            // завантажуємо обкладинку подкасту
            if (!ImportedPodcastCoverToStore(tmpstrUrl, imgUrl, cd)) {
                logger.info("Завантаження файлу Cover при імпорті подкасту: Проблема збереження");
            }
        }

        NodeList items = doc.getElementsByTagName("item");
//        for (Integer i = 0; i < items.getLength(); i++) {
        for (Integer i = 0; i < 2; i++) {

            Element item = (Element) items.item(i); // взяли епізод з RSS
            String title = getElementValue(item, "title");
            logger.info("Title {} : {}", i, title);
            // Перевіряємо, чи є епізод з такою назвою
            if (podcastService.CheckEpisodeWithTitle(title) != null) {
                logger.info("Епізод вже завантажено: {}", title);
                continue;
            }

            PodcastItem podcastItem = new PodcastItem();
            podcastItem.setClientdetail(cd);
            // зберегли новий епізод в базу, щоб можна було додавати файли
            tmpstrUrl.getPodcastChannel().getItem().add(podcastItem);

            podcastItem.setTitle(title);

            podcastItem.setDescription(getElementValue(item, "description"));

            String audioUrl = getAttributeValue(item, "enclosure", "url"); // Посилання на аудіофайл
            logger.info("audioUrl:{}", audioUrl);

            /* Завантажуємо файл для відтворення епізода у сховище */
            if (!ImportedEpisodeEnclosureToStore(tmpstrUrl, audioUrl, podcastItem, cd)) {
                // не зберігаємо епізод при помилці завантаження файлу з епізодом
                logger.info("===== Щось пішло не так при завантаженні епізоду\n"
                        + "     enclosure url: {}"
                        + "     Епізод: {}"
                        + "     Подкаст: {}", audioUrl, title, podcastTitle);
                continue;
            }
            // зберегли оригінальне посилання на enclosure
            podcastItem.setOriginalenclosure(audioUrl);

            // itunes:image
            String episodeImageUrl = getAttributeValue(item, "itunes:image", "href"); // Посилання на аудіофайл
            logger.info("episodeImageUrl:{}", episodeImageUrl);
            if (!ImportedEpisodeImageToStore(tmpstrUrl, episodeImageUrl, podcastItem, cd)) {
                // не зберігаємо епізод при помилці завантаження файлу з епізодом
                logger.info("===== Щось пішло не так при завантаженні картинки епізоду\n"
                        + "     image url: {}"
                        + "     Епізод: {}"
                        + "     Подкаст: {}", episodeImageUrl, title, podcastTitle);
                continue;
            }
//            podcastItem.set(episodeImageUrl);

//            tmpstrUrl.getPodcastChannel().getItem().add(podcastItem);
        }

        /* зберегли весь подкаст з епізодами */
        podcastService.SavePodcast(tmpstrUrl.getPodcastChannel());

        model.addAttribute("strUrl", tmpstrUrl);
        return "/podcast/getRSSFromUrl";
    }

    private boolean ImportedEpisodeImageToStore(strUrl tmpstrUrl, String episodeImageUrl, PodcastItem podcastItem, Clientdetail cd) {
        // витягуємо оригінальне імʼя файлу
        DownloadFileResult resultDownloadFile;
        // зберігаємо в базі
        try {
            // завантажуємо картинку
            resultDownloadFile = downloadFile(episodeImageUrl);
            PodcastChannel podcast = tmpstrUrl.getPodcastChannel();
            String storeUUID = storeService.PutFileToStore(resultDownloadFile.inputStream, resultDownloadFile.fileName, cd, STORE_PODCASTCOVER);
            podcastService.SaveCoverEpisodeUploadfile(storeUUID, podcastItem, cd);
            podcastItem.getImage().setOriginalurl(episodeImageUrl);

        } catch (DownloadFileException e) {
            logger.info("Завантаження файлу Cover подкасту: Проблема збереження");
            return false;
//                e.printStackTrace();
        }
        logger.info("uploaded file {}", resultDownloadFile.fileName);
        return true;
    }

    // завантажуємо картинку подкасту до сховища
    private boolean ImportedPodcastCoverToStore(strUrl tmpstrUrl, String coverurl, Clientdetail cd) {
        // витягуємо оригінальне імʼя файлу
        DownloadFileResult resultDownloadFile;
        // зберігаємо в базі
        try {
            // завантажуємо картинку
            resultDownloadFile = downloadFile(coverurl);
            PodcastChannel podcast = tmpstrUrl.getPodcastChannel();
            String storeUUID = storeService.PutFileToStore(resultDownloadFile.inputStream, resultDownloadFile.fileName, cd, STORE_PODCASTCOVER);
            podcastService.SaveCoverPodcastUploadfile(storeUUID, tmpstrUrl.getPodcastChannel(), cd);
        } catch (DownloadFileException e) {
            logger.info("Завантаження файлу Cover подкасту: Проблема збереження");
            return false;
//                e.printStackTrace();
        }
        logger.info("uploaded file {}", resultDownloadFile.fileName);
        return true;
    }

    // завантажуємо епізод до сховища
    private boolean ImportedEpisodeEnclosureToStore(strUrl tmpstrUrl, String audioUrl, PodcastItem podcastItem, Clientdetail cd) {
//            // завантажуємо епізоди

        // витягуємо оригінальне імʼя файлу
        DownloadFileResult resultDownloadFile;
        // зберігаємо в базі
        PodcastChannel podcast = tmpstrUrl.getPodcastChannel();
        try {
            resultDownloadFile = downloadFile(audioUrl);
            String storeUUID = storeService.PutFileToStore(resultDownloadFile.inputStream, resultDownloadFile.fileName, cd, STORE_EPISODETRACK);
//                podcastService.SaveEpisodeUploadfile(storeUUID, podcast, cd);

            // заносимо інформацію в епізод
            podcastItem.setChanel(podcast);
            podcastItem.setStoreuuid(storeUUID);
            podcastItem.setStoreitem(storeService.GetStoreByUUID(storeUUID));
            podcastItem.setClientdetail(cd);
            podcastItem.setTimetrack(podcastService.GetTimeTrack(storeUUID)); // зберегли час треку для RSS
        } catch (DownloadFileException e) {
            logger.info("Завантаження файлу імпортованого епізоду: Проблема збереження");
            return false;
        }

        return true;
    }

    // Читаємо RSS
    private String fetchRssContent(String rssUrl) {
        DownloadFileResult resultDownloadFile;
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
