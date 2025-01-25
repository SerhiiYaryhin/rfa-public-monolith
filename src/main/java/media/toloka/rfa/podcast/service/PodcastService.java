package media.toloka.rfa.podcast.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import media.toloka.rfa.podcast.model.PodcastItunesCategory;
import media.toloka.rfa.podcast.repositore.ItunesCategoryRepository;
import media.toloka.rfa.radio.dropfile.service.FilesService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.podcast.model.PodcastChannel;
import media.toloka.rfa.podcast.model.PodcastImage;
import media.toloka.rfa.podcast.model.PodcastItem;
import media.toloka.rfa.podcast.repositore.ChanelRepository;
import media.toloka.rfa.podcast.repositore.EpisodeRepository;
import media.toloka.rfa.podcast.repositore.PodcastCoverRepository;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.Store;
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
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.*;

@Service
public class PodcastService {

    final Logger logger = LoggerFactory.getLogger(PodcastService.class);


    @Autowired
    private ChanelRepository chanelRepository;

    @Autowired
    private EpisodeRepository episodeRepository;

    @Autowired
    private ItunesCategoryRepository itunesCategoryRepository;

    @Autowired
    private PodcastCoverRepository coverPodcastRepository;

    @Autowired
    private StoreService storeService;

    @Autowired
    private FilesService filesService;

    public PodcastChannel GetChanelByUUID(String puuid) {
        return chanelRepository.getByUuid(puuid);
    }

    public void SavePodcast(PodcastChannel podcast) {
        chanelRepository.save(podcast);
    }

    public List<PodcastChannel> GetPodcastListByCd(Clientdetail cd) {
        return chanelRepository.findByClientdetail(cd);
    }

    public PodcastItem GetEpisodeByUUID(String euuid) {
        return episodeRepository.getByUuid(euuid);
    }

    public void SaveEpisodeUploadfile(String storeUUID, PodcastChannel podcast, Clientdetail cd) {
        // зберігаємо інформацію про завантажений епізод
        PodcastItem episode = new PodcastItem();
        episode.setChanel(podcast);
        episode.setStoreuuid(storeUUID);
        episode.setStoreitem(storeService.GetStoreByUUID(storeUUID));
        episode.setClientdetail(cd);
        episode.setTimetrack(GetTimeTrack(storeUUID)); // зберегли час треку для RSS
        podcast.getItem().add(episode);

        SavePodcast(podcast);

//        episodeRepository.save(episode);
    }

    public String GetTimeTrack(String storeUUID) {
        // Беремо в сховищі завантажений трек і визначаємо його тривалість
        String cursFile = storeService.GetStoreByUUID (storeUUID).getFilepatch ();
        String resultLength;


        try {
//            File file =
            AudioFile audioMetadata = AudioFileIO.read(new File(cursFile));
            logger.info("Audio file {}",cursFile);
            logger.info("Audio TrackLength {}",audioMetadata.getAudioHeader().getTrackLength ());
            Integer iii = audioMetadata.getAudioHeader().getTrackLength ();
            Long lll = Long.valueOf(iii * 1000);
            Date ddd = new Date(lll);

            Integer hours = audioMetadata.getAudioHeader().getTrackLength () / 3600;
            Integer minutes = (audioMetadata.getAudioHeader().getTrackLength () % 3600) / 60;
            Integer seconds = audioMetadata.getAudioHeader().getTrackLength () % 60;

            resultLength = String.format("%02d:%02d:%02d", hours, minutes, seconds);

            logger.info("Audio bitrate {}",audioMetadata.getAudioHeader().getBitRate());
        } catch (CannotReadException | IOException | TagException | ReadOnlyFileException
                 | InvalidAudioFrameException e) {
            throw new RuntimeException("Error while getting metadata for audio file. Error " +  e.getLocalizedMessage());
        }
        return resultLength;
    }

    /**
     * This method is to get the language code from given language name
     * as locale can't be instantiate from a language name.
     *
     * You can specify which language you are at : Locale loc=new Locale("en") use whatever your language is
     *
     * @param lng -> given language name eg.: English
     * @return -> will return "eng"
     *
     * Wilson M Penha Jr.
     * https://stackoverflow.com/questions/29632342/converting-language-names-to-iso-639-language-codes
     */
    public String GetLanguageCode(String lng){
        if (lng == null) return "ukr";
        Locale loc = new Locale("en");
        String[] name = loc.getISOLanguages(); // list of language codes

        for (int i = 0; i < name.length; i++) {
            Locale locale = new Locale(name[i],"US");
            // get the language name in english for comparison
//            String langLocal = itemLang.toLowerCase();
            String langLocal = locale.getDisplayLanguage(loc).toLowerCase();
            if (lng.toLowerCase().equals(langLocal)){
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
        PodcastImage podcastImage = new PodcastImage();
        podcastImage.setStoreidimage(storeService.GetStoreByUUID(storeUUID));
        podcastImage.setClientdetail(cd);
        podcast.setImage(podcastImage);
        SavePodcast(podcast);
    }

    public void SaveCoverEpisodeUploadfile(String storeUUID, PodcastItem episode, Clientdetail cd) {
        PodcastImage itemImage = new PodcastImage();
        itemImage.setStoreidimage(storeService.GetStoreByUUID(storeUUID));
        itemImage.setClientdetail(cd);
        episode.setImage(itemImage);
        for (PodcastItem item : episode.getChanel().getItem()) {
            if (item.getId() == episode.getId() ) {
                item.setImage(itemImage);
                break;
            }
        }
        SavePodcast(episode.getChanel());
    }

    public List<PodcastItem> GetAllEpisodePaging(Clientdetail cd) {
        // findByClientdetailAndStorefiletypeOrderByIdDesc(cd,STORE_EPISODETRACK)
        return episodeRepository.findByClientdetailOrderByIdDesc(cd);
    }

    public List<PodcastImage> GetPodcastCoverListByCd(Clientdetail cd) {
        return coverPodcastRepository.findByClientdetailOrderByIdDesc(cd);
    }

    public PodcastImage GetImageByUUID(String iuuid) {
        return coverPodcastRepository.getByUuid(iuuid);
    }

    public List<PodcastChannel> GetPodcastListForRootCarusel() {
//        return chanelRepository.findByApruve(true);
//        return chanelRepository.findByPublishing(true);
        List<PodcastChannel> jjj = chanelRepository.findByPublishing(true);
        return jjj;
    }

    public List<PodcastChannel> GetAllChanel() {
        List<PodcastChannel> listCh = chanelRepository.findAll();
        return listCh;
    }

    public String GetEpisodeNumberComments(PodcastItem item) {
        // кількість коментарів для епізоду подкасту
        return "0";
    }

    // читаємо категорії itunes для подкасту з файлу, який розташовано в ресурсах
    public Map<String,List<String>> ItunesCategory() {

        String resource = "itunes.json";
        String jsonString;
        try {
            ClassLoader cl = ClassUtils.getDefaultClassLoader();
            File file = ResourceUtils.getFile("classpath:"+resource);
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
        return new Gson().fromJson(jsonString, new TypeToken<HashMap<String, Object>>() {}.getType());
    }

    public void SaveItunesCategory(PodcastItunesCategory pic) {
        itunesCategoryRepository.save(pic);
    }

    public void ItunesCategoryClear(PodcastItunesCategory toclear) {
        itunesCategoryRepository.delete(toclear);
    }

    public List<PodcastItem> GetListByTitle(String title) {
        return episodeRepository.findByTitle(title);
    }

    public List<PodcastChannel> GetChanelByTitle(String podcastTitle) {
        List<PodcastChannel> podcastChannelList = chanelRepository.findByTitle(podcastTitle);
        if( podcastChannelList.isEmpty() ) return null;
        return podcastChannelList;
    }
}
