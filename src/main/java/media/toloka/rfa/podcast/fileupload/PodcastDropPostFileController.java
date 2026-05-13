package media.toloka.rfa.podcast.fileupload;

import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.dropfile.service.FilesService;
import media.toloka.rfa.radio.history.service.HistoryService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.podcast.model.PodcastChannel;
import media.toloka.rfa.podcast.model.PodcastItem;
import media.toloka.rfa.podcast.service.PodcastService;
import media.toloka.rfa.radio.store.Service.StoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import static media.toloka.rfa.radio.store.model.EStoreFileType.*;


@Slf4j
@RestController
//@RequestMapping("/uploadfile")
public class PodcastDropPostFileController {

    @Value("${media.toloka.rfa.upload_directory}")
    private String PATHuploadDirectory;

    @Autowired
    private ClientService clientService;

    @Autowired
    private PodcastService podcastService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private FilesService filesService;

    @Autowired
    private StoreService storeService;

    final Logger logger = LoggerFactory.getLogger(PodcastDropPostFileController.class);

    @PostMapping(path = "/podcast/episodeupload/{puuid}" )
    public ResponseEntity<?> EpisodeUpload(
            @PathVariable String puuid,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            logger.info("Завантаження епізоду подкасту: Файл порожній");
            return ResponseEntity.badRequest().body(Map.of("error", "Empty file"));
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (clientService.ClientCanDownloadFile(cd) == false) {
            logger.warn("Клієнт {} не має права завантажувати файли.", cd.getUuid());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
        }
        PodcastChannel podcast = podcastService.GetChanelByUUID(puuid);
        try {
            String storeUUID = storeService.PutFileToStore(file.getInputStream(),file.getOriginalFilename(),cd,STORE_EPISODETRACK);
            PodcastItem episode = new PodcastItem();
            episode.setChanel(podcast);
            episode.setStoreuuid(storeUUID);
            episode.setEnclosurestore(storeService.GetStoreByUUID(storeUUID));
            episode.setClientdetail(cd.getUuid());
            episode.setTimetrack(podcastService.GetTimeTrack(storeUUID));
            podcast.getItem().add(episode);

            podcastService.SavePodcast(podcast);
            
            return ResponseEntity.ok(Map.of("success", true, "uuid", storeUUID, "episodeUuid", episode.getUuid()));
        } catch (IOException e) {
            logger.error("Завантаження файлу: Проблема збереження", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // Завантажуємо обкладинку подкасту

    /**
     * завантажуємо обкладинку подкасту
     * @param puuid uuid родкасту
     * @param file файл з броузера клієнта, що завантажуємо
     */
    @PostMapping(path = "/podcast/podcastcoverupload/{puuid}" )
    public ResponseEntity<?> PodcastCoverUpload(
            @PathVariable String puuid,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            logger.warn("PodcastCoverEpisodeUpload: Файл, що завантажуємо порожній");
            return ResponseEntity.badRequest().body(Map.of("error", "Empty file"));
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (clientService.ClientCanDownloadFile(cd) == false) {
            logger.warn("Клієнт {} не має права завантажувати файли.", cd.getUuid());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
        }
        PodcastChannel podcast = podcastService.GetChanelByUUID(puuid);
        try {
            String storeUUID = storeService.PutFileToStore(file.getInputStream(),file.getOriginalFilename(),cd,STORE_PODCASTCOVER);
            podcastService.SaveCoverPodcastUploadfile(storeUUID, podcast, cd);
            return ResponseEntity.ok(Map.of("success", true, "uuid", storeUUID));
        } catch (IOException e) {
            logger.error("Завантаження файлу: Проблема збереження", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Зберігаємо завантажену обкладинку для епізоду подкасту
     * @param puuid uuid родкасту
     * @param euuid uuid епізоду
     * @param file файл з броузера клієнта, що завантажуємо
     */
    @PostMapping(path = "/podcast/podcastcoverepisodeupload/{puuid}/{euuid}" )
    public ResponseEntity<?> PodcastCoverEpisodeUpload(
            @PathVariable String puuid,
            @PathVariable String euuid,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            logger.warn("PodcastCoverEpisodeUpload: Файл обкладинки порожній");
            return ResponseEntity.badRequest().body(Map.of("error", "Empty file"));
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (clientService.ClientCanDownloadFile(cd) == false) {
            logger.warn("PodcastCoverEpisodeUpload: Клієнт {} не має права завантажувати файли.", cd.getUuid());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
        }
        PodcastChannel podcast = podcastService.GetChanelByUUID(puuid);
        PodcastItem podcastItem = podcastService.GetEpisodeByUUID(euuid);
        try {
            String storeUUID = storeService.PutFileToStore(file.getInputStream(),file.getOriginalFilename(),cd,STORE_PODCASTCOVER);
            podcastItem.setImagestoreitem(storeService.GetStoreByUUID(storeUUID));
            podcastService.SavePodcast(podcast);
            return ResponseEntity.ok(Map.of("success", true, "uuid", storeUUID));
        } catch (IOException e) {
            logger.error("PodcastCoverEpisodeUpload: Завантаження файлу: Проблема збереження", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

}

