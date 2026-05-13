package media.toloka.rfa.podcast.fileupload;

import media.toloka.rfa.podcast.model.PodcastChannel;
import media.toloka.rfa.podcast.model.PodcastItem;
import media.toloka.rfa.podcast.service.PodcastService;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.Store;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/podcast/link")
public class PodcastFileLinkController {

    @Autowired
    private PodcastService podcastService;

    @Autowired
    private StoreService storeService;

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

    // Прив'язка аудіофайлу до епізоду
    @PostMapping("/episode-audio/{puuid}")
    public ResponseEntity<?> linkAudioToEpisode(@PathVariable String puuid, @RequestParam String storeUuid) {
        PodcastChannel podcast = podcastService.GetChanelByUUID(puuid);
        Store store = storeService.GetStoreByUUID(storeUuid);

        if (podcast == null || store == null) {
            return ResponseEntity.notFound().build();
        }

        PodcastItem episode = new PodcastItem();
        episode.setChanel(podcast);
        episode.setStoreuuid(storeUuid);
        episode.setEnclosurestore(store);
        episode.setTimetrack(podcastService.GetTimeTrack(storeUuid));
        
        podcast.getItem().add(episode);
        podcastService.SavePodcast(podcast);

        return ResponseEntity.ok(Map.of("success", true, "episodeUuid", episode.getUuid()));
    }
}
