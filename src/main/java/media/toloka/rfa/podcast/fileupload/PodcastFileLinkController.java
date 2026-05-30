package media.toloka.rfa.podcast.fileupload;

import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.podcast.model.PodcastChannel;
import media.toloka.rfa.podcast.model.PodcastItem;
import media.toloka.rfa.podcast.service.PodcastService;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.Store;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/podcast/link")
public class PodcastFileLinkController {

    @Autowired
    private PodcastService podcastService;

    @Autowired
    private StoreService storeService;

    @Autowired
    private ClientService clientService;

    // Прив'язка обкладинки до подкасту
    @PostMapping("/cover/{puuid}")
    public ResponseEntity<?> linkCoverToPodcast(@PathVariable String puuid, @RequestParam String storeUuid) {
        PodcastChannel podcast = podcastService.GetChanelByUUID(puuid);
        Store store = storeService.GetStoreByUUID(storeUuid);

        if (podcast == null || store == null) {
            return ResponseEntity.notFound().build();
        }

        podcast.setImagechanelstore(store);
        podcastService.SavePodcast(podcast);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // Прив'язка обкладинки до епізоду
    @PostMapping("/episode-cover/{euuid}")
    public ResponseEntity<?> linkCoverToEpisode(@PathVariable String euuid, @RequestParam String storeUuid) {
        PodcastItem episode = podcastService.GetEpisodeByUUID(euuid);
        Store store = storeService.GetStoreByUUID(storeUuid);

        if (episode == null || store == null) {
            return ResponseEntity.notFound().build();
        }

        episode.setImagestoreitem(store);
        podcastService.SaveEpisode(episode);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // Прив'язка аудіофайлу до існуючого епізоду
    @PostMapping("/episode-audio-link/{euuid}")
    public ResponseEntity<?> linkAudioToExistingEpisode(@PathVariable String euuid, @RequestParam String storeUuid) {
        PodcastItem episode = podcastService.GetEpisodeByUUID(euuid);
        PodcastChannel chanel = podcastService.GetChanelByUUID(storeUuid);
        Store store = storeService.GetStoreByUUID(storeUuid); // з якого дива ми подкаст витягуємо зі сховища?

        if (episode == null || chanel == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Episode or Store item not found"));
        }

        try {
            episode.setStoreuuid(storeUuid);
            episode.setEnclosurestore(store);
            episode.setTimetrack(podcastService.GetTimeTrack(storeUuid));
            podcastService.SaveEpisode(episode);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error linking audio: " + e.getMessage()));
        }
    }

    // Прив'язка аудіофайлу до подкасту (створення нового епізоду)
    @PostMapping("/episode-audio/{puuid}")
    public ResponseEntity<?> linkAudioToEpisode(@PathVariable String puuid, @RequestParam String storeUuid) {
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        PodcastChannel podcast = podcastService.GetChanelByUUID(puuid);
        Store store = storeService.GetStoreByUUID(storeUuid);

        if (podcast == null || store == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Podcast or Store item not found"));
        }

        // Перевірка: чи вже існує епізод з таким файлом у цьому подкасті
        boolean exists = podcast.getItem().stream()
                .anyMatch(item -> item.getStoreuuid() != null && item.getStoreuuid().equals(storeUuid));

        if (exists) {
            return ResponseEntity.badRequest().body(Map.of("error", "Episode with this audio file already exists in this podcast"));
        }

        try {
            PodcastItem episode = new PodcastItem();
            episode.setChanel(podcast);
            episode.setStoreuuid(storeUuid);
            episode.setEnclosurestore(store);
            episode.setClientdetail(cd.getUuid());
            episode.setTimetrack(podcastService.GetTimeTrack(storeUuid));
            episode.setUuid(UUID.randomUUID().toString());
            
            podcast.getItem().add(episode);
            podcastService.SavePodcast(podcast);

            return ResponseEntity.ok(Map.of("success", true, "episodeUuid", episode.getUuid()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error creating episode: " + e.getMessage()));
        }
    }
}
