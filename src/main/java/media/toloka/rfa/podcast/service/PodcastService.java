package media.toloka.rfa.podcast.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.Setter;
import media.toloka.rfa.podcast.model.PodcastItunesCategory;
import media.toloka.rfa.podcast.repositore.ItunesCategoryRepository;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.dropfile.service.FilesService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.podcast.model.PodcastChannel;
//import media.toloka.rfa.podcast.model.PodcastImage;
import media.toloka.rfa.podcast.model.PodcastItem;
import media.toloka.rfa.podcast.repositore.ChanelRepository;
import media.toloka.rfa.podcast.repositore.EpisodeRepository;
//import media.toloka.rfa.podcast.repositore.PodcastCoverRepository;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.service.DownloadFileException;
import media.toloka.rfa.service.DownloadFileResult;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;

import static media.toloka.rfa.radio.store.model.EStoreFileType.STORE_EPISODETRACK;
import static media.toloka.rfa.radio.store.model.EStoreFileType.STORE_PODCASTCOVER;
import static media.toloka.rfa.service.FileDownloader.downloadFile;

@Service
public class PodcastService {

    final Logger logger = LoggerFactory.getLogger(PodcastService.class);


    @Autowired
    private ChanelRepository chanelRepository;

    @Autowired
    private EpisodeRepository episodeRepository;

    @Autowired
    private ItunesCategoryRepository itunesCategoryRepository;

//    @Autowired
//    private PodcastCoverRepository coverPodcastRepository;

    @Autowired
    private StoreService storeService;

    @Autowired
    private FilesService filesService;

    @Autowired
    private ClientService clientService;

    public Store GetStoreByUUID(String iuuid) {
        return storeService.GetStoreByUUID(iuuid);
    }

    public Clientdetail GetCdByUUID(PodcastChannel podcastChannel) {
        return clientService.GetClientDetailByUUID(podcastChannel.getClientdetail());
    }

    public PodcastItem GetEpisodeByTitle(String euuid) {
        return episodeRepository.getByTitle(euuid);
    }

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


    public PodcastChannel GetChanelByUUID(String puuid) {
        return chanelRepository.getByUuid(puuid);
    }

    public void SavePodcast(PodcastChannel podcast) {
        chanelRepository.save(podcast);
    }

    public List<PodcastChannel> GetPodcastListByCd(Clientdetail cd) {
        return chanelRepository.findByClientdetail(cd.getUuid());
    }

    public PodcastItem GetEpisodeByUUID(String euuid) {
        return episodeRepository.getByUuid(euuid);
    }

//    public void SaveEpisodeUploadfile(String storeUUID, PodcastChannel podcast, Clientdetail cd) {
//        // створюємо епізод, записуємо в нього інформацію про завантажений епізод та зберігаємо в базі
//        PodcastItem episode = new PodcastItem();
//        episode.setChanel(podcast);
//        episode.setStoreuuid(storeUUID);
//        episode.setEnclosurestore(storeService.GetStoreByUUID(storeUUID));
//        episode.setClientdetail(cd.getUuid());
//        episode.setTimetrack(GetTimeTrack(storeUUID)); // зберегли час треку для RSS
//        podcast.getItem().add(episode);
//
//        SavePodcast(podcast);
//    }

    /**
     * Отримуємо час тривалості треку
     * @param storeUUID
     * @return
     */
    public String GetTimeTrack(String storeUUID) {
        // Беремо в сховищі завантажений трек і визначаємо його тривалість
        String cursFile = storeService.GetStoreByUUID(storeUUID).getFilepatch();
        String resultLength;
        try {
            AudioFile audioMetadata = AudioFileIO.read(new File(cursFile));
            logger.info("Audio file {}", cursFile);
            logger.info("Audio TrackLength {}", audioMetadata.getAudioHeader().getTrackLength());
            Integer iii = audioMetadata.getAudioHeader().getTrackLength();
            Long lll = Long.valueOf(iii * 1000);
            Date ddd = new Date(lll);

            Integer hours = audioMetadata.getAudioHeader().getTrackLength() / 3600;
            Integer minutes = (audioMetadata.getAudioHeader().getTrackLength() % 3600) / 60;
            Integer seconds = audioMetadata.getAudioHeader().getTrackLength() % 60;
            resultLength = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            logger.info("Audio bitrate {}", audioMetadata.getAudioHeader().getBitRate());
        } catch (CannotReadException | IOException | TagException | ReadOnlyFileException
                 | InvalidAudioFrameException e) {
            throw new RuntimeException("Помилка при визначенні тривалості треку. " + e.getLocalizedMessage());
        }
        return resultLength;
    }

    /**
     * This method is to get the language code from given language name
     * as locale can't be instantiate from a language name.
     * <p>
     * You can specify which language you are at : Locale loc=new Locale("en") use whatever your language is
     *
     * @param lng -> given language name eg.: English
     * @return -> will return "eng"
     * <p>
     * Wilson M Penha Jr.
     * https://stackoverflow.com/questions/29632342/converting-language-names-to-iso-639-language-codes
     */
    public String GetLanguageCode(String lng) {
        if (lng == null) return "ukr";
        Locale loc = new Locale("en");
        String[] name = loc.getISOLanguages(); // list of language codes

        for (int i = 0; i < name.length; i++) {
            Locale locale = new Locale(name[i], "US");
            // get the language name in english for comparison
//            String langLocal = itemLang.toLowerCase();
            String langLocal = locale.getDisplayLanguage(loc).toLowerCase();
            if (lng.toLowerCase().equals(langLocal)) {
                return locale.getISO3Language();
            }
        }
        return "unknown";
    }

    public void SaveEpisode(PodcastItem episode) {
        // todo Записати час файлу для RSS XML
        episodeRepository.save(episode);
    }

    public void SaveCoverPodcastUploadfile(String storeUUID, PodcastChannel podcast, Clientdetail cd) {
        // змінив під нову структуру подкастів в базі. 250202
//        PodcastImage podcastImage = new PodcastImage();
        podcast.setImagechanelstore(storeService.GetStoreByUUID(storeUUID));
//        podcastImage.setStoreidimage(podcast.getImagestoreitem());
//        podcastImage.setClientdetail(cd);
//        podcast.setImage(podcastImage);
        SavePodcast(podcast);
    }

//    public void SaveCoverEpisodeUploadfile(String storeUUID, PodcastItem episode, Clientdetail cd) {
//        PodcastImage itemImage = new PodcastImage();
//        itemImage.setStoreidimage(storeService.GetStoreByUUID(storeUUID));
//        itemImage.setClientdetail(cd);
//        episode.setImagestoreitem(storeService.GetStoreByUUID(storeUUID));
//        for (PodcastItem item : episode.getChanel().getItem()) {
//            if (item.getId() == episode.getId()) {
//                item.setImage(itemImage);
//                break;
//            }
//        }
//        SavePodcast(episode.getChanel());
//    }

    /**
     * Вибираємо всі епізоди зі всіх подкастів для цього клієта
     * @param cd наш клієнт
     * @return List<PodcastItem>
     */
    public List<PodcastItem> GetAllEpisodePaging(Clientdetail cd) {
        return episodeRepository.findByClientdetailOrderByIdDesc(cd);
    }

    /**
     * Отримуємо перелік всіх обкладинок подкастів та епізодів від конкретного користувача в системі
     * @param cd конкретний користувач, який розміщувал подкасти.
     * @return
     */
    public List<Store> GetPodcastCoverListByCd(Clientdetail cd) {
        return storeService.GetPodcastCoverListByCd(cd);
    }


    /**
     * Беремо з бази всі подкасти що схвалені для каруселі на головній сторінці
     * @return List<PodcastChannel>
     */
    public List<PodcastChannel> GetPodcastListForRootCarusel() {
//        List<PodcastChannel> jjj = chanelRepository.findByPublishing(true);
        return chanelRepository.findByPublishing(true);
    }

    /**
     * Отримати всі подкасти з бази
     * @return List<PodcastChannel>
     */
    public List<PodcastChannel> GetAllChanel() {
        List<PodcastChannel> listCh = chanelRepository.findAll();
        return listCh;
    }

    public String GetEpisodeNumberComments(PodcastItem item) {
        // кількість коментарів для епізоду подкасту
        return "0";
    }

    // читаємо категорії itunes для подкасту з файлу, який розташовано в ресурсах
    public Map<String, List<String>> ItunesCategory() {

        String resource = "itunes.json";
        String jsonString;
        try {
            ClassLoader cl = ClassUtils.getDefaultClassLoader();
            File file = ResourceUtils.getFile("classpath:" + resource);
            logger.info(file.toString());
            jsonString = new String(Files.readAllBytes(file.toPath()));
        } catch (FileNotFoundException e) {
            logger.info("FileNotFoundException: Щось пішло не так під час читання файлу переліку категорій для ITunes.");
            return null;
        } catch (IOException e) {
            logger.info("IOException: Щось пішло не так під час читання файлу переліку категорій для ITunes.");
            return null;
        }
        // отримали рядок з файлу
        return new Gson().fromJson(jsonString, new TypeToken<HashMap<String, Object>>() {
        }.getType());
    }

    public void SaveItunesCategory(PodcastItunesCategory itCategory) {
        itunesCategoryRepository.save(itCategory);
    }

    public void ItunesCategoryClear(PodcastItunesCategory toclear) {
        itunesCategoryRepository.delete(toclear);
    }

    /**
     * шукаємо серед епізодів таку назву.
     * Додати пошук з урахуванням поточного користувача лише серед його подкастів
     * епізодів може бути декілька.
     * @param title String назва епізоду
     * @return List<PodcastItem>
     */
    public List<PodcastItem> GetListByTitle(String title) {
        return episodeRepository.findByTitle(title);
    }

    /**
     * Шукаємо в базі подкаст з такою назвою
     * може статися так, що два різних подкасти будуть мати одну назву.
     * Передбачити обробку цієї ситуації
     * Додати пошук з урахуванням поточного користувача лише серед його подкастів
     * @param podcastTitle String з назвою подкасту
     * @return List<PodcastChannel>
     */
    public List<PodcastChannel> GetChanelByTitle(String podcastTitle) {
        List<PodcastChannel> podcastChannelList = chanelRepository.findByTitle(podcastTitle);
        if (podcastChannelList.isEmpty()) return null;
        return podcastChannelList;
    }

    /**
     * шукаємо серед епізодів таку назву.
     * Додати пошук з урахуванням поточного користувача лише серед його подкастів
     * епізодів може бути декілька.
     * @param title String назва епізоду
     * @return PodcastItem
     */
    public PodcastItem CheckEpisodeWithTitle(String title) {
        return episodeRepository.getByTitle(title);
    }

    /*

     */

    /**
     * Видаляємо подкаст та вичіщаємо всі записи і файли зі сховища
     * Проконтролювати, чи використовується цей файл в інших сутностях
     * Наприклад, картинка подкасту може використовуватися в пості з анонсом виходу нового епізоду.
     * Лічильник кількості посилань на обʼєкт у сховищі.
     * @param lChannel Канал подкасту, який існує у базі
     * @return Boolean успішно/неуспішно
     */
    public Boolean ClearAndDeletePodcastChanel(PodcastChannel lChannel) {
        // перелік Store, які видаляємо у самому кінці обробки.
        List<Store> storeListToRemove = new ArrayList<>();

        List<PodcastItem> podcastItemList = lChannel.getItem();
//        lChannel.getItem().clear();
        for (PodcastItem pi : podcastItemList) { // Get подкаст айтем
            // видаляємо аудіо файл з епізодом
            logger.info("=================\nПрацюєемо з епізодом: {}\n{}\n{}", pi.getId(), pi.getUuid(), pi.getTitle());
            Store st = pi.getEnclosurestore();  // Взяли посилання на епізод у сховищі
            if (st != null) {
                storeListToRemove.add(st);
                logger.info("Видаляємо аудіофайл епізоду із сховища: {}", st.getFilepatch());
                if (storeService.DeleteInStore(st)) {
                    pi.setEnclosurestore(null);
                    logger.info("Видалили аудіофайл епізоду із сховища");
                }
            }
            // видаляємо файл з обкладинкою епізоду
            Store piImage = pi.getImagestoreitem();
            if (piImage != null) {
                storeListToRemove.add(piImage);
                logger.info("Видаляємо обкладинку епізоду із сховища: {}", piImage.getFilepatch());
                if (storeService.DeleteInStore(st)) {
                    pi.setImagestoreitem(null);
                    logger.info("Видалили обкладинку епізоду із сховища");
                }
            }
        } // Закінчили працювати з епізодами
        lChannel.getItem().clear();
        SavePodcast(lChannel);

        // почистили все в елементі
        // видаляємо його.
//            podcastItemList.remove(pi);

        // видаляємо cover для подкасту
        if (lChannel.getImagechanelstore() != null) {
            Store simg = lChannel.getImagechanelstore(); // Взяли посилання на картинку подкасту у сховищі
            storeListToRemove.add(simg);
            if (simg != null) {
                if (storeService.DeleteInStore(simg)) {
                    logger.info("Видалили обкладинку епізоду із сховища");
                    lChannel.setImagechanelstore(null);
                }
            }
        }

        List<PodcastItunesCategory> podcastItunesCategoryList = lChannel.getItunescategory();
        podcastItunesCategoryList.clear();
//        lChannel.setClientdetail(null);
//        PodcastImage podcastImage = lChannel.getImage();
//        lChannel.setImage(null);
//        podcastImage.setStoreidimage(null);
//        coverPodcastRepository.save(podcastImage);
//        coverPodcastRepository.delete(podcastImage);

        try {
            SavePodcast(lChannel);
            chanelRepository.delete(lChannel);
        } catch (
                Exception e) {
            logger.info("Проблема під час фінального видалення подкасту!!!");
            return false;
        }

        // чистимо залишки у сховищі
        for(Store rst : storeListToRemove) {
            if (storeService.GetStoreByUUID(rst.getUuid()) != null) {
                storeService.DeleteInStore(storeService.GetStoreByUUID(rst.getUuid()));
            }
        }

        return true;
    }

//private void DelPodcastImage(PodcastImage piImage) {
//    coverPodcastRepository.delete(piImage);
//}

    /**
     * Завантажуємо подкаст на наш сервер
     * @param model
     * @param gstrUrl в цьому класі міститься String з адресою RSS
     */
    public void PutPodcastFromRSS(Model model, strUrl gstrUrl) {

        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());

// тимчасово закоментував
//        gstrUrl.setTested(false);
//        if (gstrUrl.getTested()) {
//            PodcastChannel mypc = GetChanelByUUID("402f655b-addb-4faa-952d-e444952a8dc7");
//            List<PodcastItem> mypodcastItem = mypc.getItem();
//            PodcastItem mypi;
//            mypi = GetEpisodeByUUID("bde7528c-f1d0-4860-9485-5b7b886e8a2b");
//            mypodcastItem.add(mypi);
//            mypi = GetEpisodeByUUID("8fcd627f-30d8-43a8-8001-8051e941a8ab");
//            mypodcastItem.add(mypi);
//            SavePodcast(mypc);
//
//            model.addAttribute("strUrl", gstrUrl);
//            return;
//        }

        // Створили пусту структуру
        // створили нову структуру, що щось не зіпсувати.
        // у майбутньому - прибрати створення і користуватися переданою.
        strUrl tmpstrUrl = new strUrl();

        tmpstrUrl.setRSSFromUrl(gstrUrl.getRSSFromUrl());
        tmpstrUrl.setClrpodcast(gstrUrl.getClrpodcast());
//        logger.info("===== {}", tmpstrUrl.getRSSFromUrl());

        String rssContent;
        // за переданим посиланням отримуємо тіло RSS
        rssContent = fetchRssContent(tmpstrUrl.getRSSFromUrl());

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
            return;
//            return null;
        } catch (SAXException e) {
            logger.info("SAXException: Помилка перетворення на XML");
            return;
        } catch (IOException e) {
            logger.info("IOException: Помилка перетворення на XML");
            return;
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
        podcastChannelList = GetChanelByTitle(podcastTitle);

        // Чи видаляємо подкаст?
        if (gstrUrl.getClrpodcast()) {
            /* Delete podcast */
            // шукаємо в базі подкастів подкаст з такою назвою
//            podcastChannelList = podcastService.GetChanelByTitle(podcastTitle);
            if (podcastChannelList != null) {
                for (PodcastChannel lChannel : podcastChannelList) { // пробігаємося по отриманих подкастах з такою назвою
//                    if (cd == lChannel.getClientdetail()) { // якщо подкаст належить цьому користувачу, то чистимо його
                    if (cd.getUuid().equals(lChannel.getClientdetail())) { // якщо подкаст належить цьому користувачу, то чистимо його
                        if (!ClearAndDeletePodcastChanel(lChannel)) {
                            model.addAttribute("danger", "Подкаст не Видалено.");
                        }
                        ;
                    }
                }
            } else {
                model.addAttribute("warning", "Такий подкаст не знайдено.");
            }
            model.addAttribute("strUrl", gstrUrl);

            // Закінчили видалення подкасту
            return;
        }

        tmpstrUrl.setPodcastChannel(new PodcastChannel()); //
        // шукаємо в базі подкастів з такою назвою
        podcastChannelList = GetChanelByTitle(podcastTitle);
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

//        updatePodcast = false;
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
            tmpstrUrl.getPodcastChannel().setClientdetail(cd.getUuid());
            tmpstrUrl.getPodcastChannel().setLinktoimporturl(tmpstrUrl.getRSSFromUrl());
            tmpstrUrl.getPodcastChannel().setLanguage(podcastLanguage);
            tmpstrUrl.getPodcastChannel().setCopyright(podcastCopyright);
            /* зберегли подкаст без епізодів */
            SavePodcast(tmpstrUrl.getPodcastChannel());
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
        for (Integer i = 0; i < items.getLength(); i++) {
//        for (Integer i = 0; i < 2; i++) {

            Element item = (Element) items.item(i); // взяли епізод з RSS
            String title = getElementValue(item, "title");
            logger.info("Title {} : {}", i, title);
            // Перевіряємо, чи є епізод з такою назвою
            if (CheckEpisodeWithTitle(title) != null) {
                logger.info("Епізод вже завантажено: {}", title);
                continue;
            }

            PodcastItem podcastItem = new PodcastItem();
            podcastItem.setClientdetail(cd.getUuid());
            // зберегли новий епізод в базу, щоб можна було додавати файли
//            tmpstrUrl.getPodcastChannel().getItem().add(podcastItem);

            podcastItem.setTitle(title);

            podcastItem.setDescription(getElementValue(item, "description"));
            // Навіщо зберігаємо, коли ще не додали елемент подкасту
            SavePodcast(tmpstrUrl.getPodcastChannel());

            String audioUrl = getAttributeValue(item, "enclosure", "url"); // Посилання на аудіофайл
            logger.info("audioUrl:{}", audioUrl);



            /* Завантажуємо файл для відтворення епізода у сховище */
            Store enclosure = ImportedEpisodeEnclosureToStore(tmpstrUrl, audioUrl, podcastItem, cd);
            podcastItem.setEnclosurestore(enclosure);
            if (podcastItem.getEnclosurestore() == null) {
                // не зберігаємо епізод при помилці завантаження файлу з епізодом
                logger.info("===== Щось пішло не так при завантаженні enclosure для епізоду"
                        + "\n     enclosure url: {}"
                        + "\n     Епізод: {}"
                        + "\n     Подкаст: {}", audioUrl, title, podcastTitle);
                continue;
            } else {
                // заносимо інформацію в епізод
                podcastItem.setChanel(tmpstrUrl.getPodcastChannel());
//                podcastItem.setStoreenclosure(storeService.GetStoreByUUID(storeUUID));
                podcastItem.setClientdetail(cd.getUuid());
                podcastItem.setTimetrack(GetTimeTrack(podcastItem.getEnclosurestore().getUuid())); // зберегли час треку для RSS
            }
            // зберегли оригінальне посилання на enclosure
            podcastItem.setOriginalenclosurelink(audioUrl);

            // itunes:image
            String episodeImageUrl = getAttributeValue(item, "itunes:image", "href"); // Посилання на аудіофайл
            logger.info("episodeImageUrl:{}", episodeImageUrl);
            // Завантажуємо обкладинку епізоду
            podcastItem.setImagestoreitem(ImportedEpisodeImageToStore(tmpstrUrl, episodeImageUrl, podcastItem, cd));
            if (podcastItem.getImagestoreitem() == null) {
                // не зберігаємо епізод при помилці завантаження файлу з епізодом
                logger.info("===== Щось пішло не так при завантаженні картинки епізоду\n"
                        + "\n     image url: {}"
                        + "\n     Епізод: {}"
                        + "\n     Подкаст: {}", episodeImageUrl, title, podcastTitle);
                podcastItem.setImagestoreitem(tmpstrUrl.getPodcastChannel().getImagechanelstore());
                logger.info("Для епізоду взяли картинку подкасту");
//                continue;
            }
            tmpstrUrl.getPodcastChannel().getItem().add(podcastItem);
        }

        /* зберегли весь подкаст з епізодами */
        SavePodcast(tmpstrUrl.getPodcastChannel());

        model.addAttribute("strUrl", tmpstrUrl);

    }


    private Store ImportedEpisodeImageToStore(strUrl tmpstrUrl, String episodeImageUrl, PodcastItem podcastItem, Clientdetail cd) {
        // витягуємо оригінальне імʼя файлу
        DownloadFileResult resultDownloadFile;
        // зберігаємо в базі
        try {
            // завантажуємо картинку
            logger.info("Завантажуємо файл Cover епізоду");
            resultDownloadFile = downloadFile(episodeImageUrl);
            PodcastChannel podcast = tmpstrUrl.getPodcastChannel();
            String storeUUID = storeService.PutFileToStore(resultDownloadFile.inputStream, resultDownloadFile.fileName, cd, STORE_PODCASTCOVER);
            logger.info("uploaded file {}", resultDownloadFile.fileName);
            //resultDownloadFile.inputStream.close();
            return storeService.GetStoreByUUID(storeUUID);
//            SaveCoverEpisodeUploadfile(storeUUID, podcastItem, cd);
//        podcastItem.getImage().setOriginalurl(episodeImageUrl);

        } catch (DownloadFileException e) {
            logger.info("Завантаження файлу Cover епізоду: Проблема збереження");
//            e.printStackTrace();
            return null;
        }
//        logger.info("uploaded file {}", resultDownloadFile.fileName);
//        return true;
    }

    // завантажуємо картинку подкасту до сховища
    private boolean ImportedPodcastCoverToStore(strUrl tmpstrUrl, String coverurl, Clientdetail cd) {
        // витягуємо оригінальне імʼя файлу
        DownloadFileResult resultDownloadFile;
        // зберігаємо в базі
        try {
            // завантажуємо картинку
            logger.info("Завантаження файлу Cover подкасту");
            resultDownloadFile = downloadFile(coverurl);
            PodcastChannel podcast = tmpstrUrl.getPodcastChannel();
            String storeUUID = storeService.PutFileToStore(resultDownloadFile.inputStream, resultDownloadFile.fileName, cd, STORE_PODCASTCOVER);
            SaveCoverPodcastUploadfile(storeUUID, tmpstrUrl.getPodcastChannel(), cd);
//            resultDownloadFile.inputStream.close();
        } catch (DownloadFileException e) {
            logger.info("Завантаження файлу Cover подкасту: Проблема збереження");
//            e.printStackTrace();
            return false;
        }
        logger.info("uploaded file {}", resultDownloadFile.fileName);
        return true;
    }

    // завантажуємо епізод до сховища
    private Store ImportedEpisodeEnclosureToStore(strUrl tmpstrUrl, String audioUrl, PodcastItem podcastItem, Clientdetail cd) {
        // витягуємо оригінальне імʼя аудіо файлу епізоду
        DownloadFileResult resultDownloadFile;
        try {
            // тут на другому файлі вилітаємо
            resultDownloadFile = downloadFile(audioUrl);
            String storeUUID = storeService.PutFileToStore(resultDownloadFile.inputStream, resultDownloadFile.fileName, cd, STORE_EPISODETRACK);
            Store encl = storeService.GetStoreByUUID(storeUUID);
            return encl;
        } catch (DownloadFileException e) {
            logger.error("ERROR: podcastService. ImportedEpisodeEnclosureToStore:\n======== Завантаження файлу імпортованого епізоду: Проблема збереження");
            return null;
        }
    }


    // працюємо з RSS

    /**
     * Зчитуємо тіло RSS без урахування передресацій
     * обробку переадресацій потрібно додати
     * @param rssUrl
     * @return String з тілом RSS
     */
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

    /**
     * отримуємо значення елементу
     * @param parent Елемент XML з якого витягуємо його значення.
     * @param tagName тег елементу XML значення якого ми забираємо
     * @return String значення тегу
     */
    private String getElementValue(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }

    /**
     * Отримуємо значення атрибуту тегу в батьківському елементі
     * @param parent батьківський елемент в середині якого знаходиться тег.
     * @param tagName імʼя тегу в батьківському елементі.
     * @param attributeName імʼя атрибуту тегу
     * @return String тіло атрибуту
     */
    private String getAttributeValue(Element parent, String tagName, String attributeName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Element element = (Element) nodeList.item(0);
            return element.getAttribute(attributeName); // Повертаємо значення атрибута
        }
        return null;
    }


}
