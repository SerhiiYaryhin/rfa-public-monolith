package media.toloka.rfa.podcast.service;

import lombok.Data;
import media.toloka.rfa.podcast.model.PodcastChannel;
import media.toloka.rfa.podcast.model.PodcastItem;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.store.model.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class PodcastRESTController {
    @Autowired
    private PodcastService podcastService;

    @Autowired
    private ClientService clientService;

    final Logger logger = LoggerFactory.getLogger(PodcastRESTController.class);

    @Data
    class myresponse {
        String current = "";
        String next = "";
        String title = "";
        String storeuuid = "";
        String file = "";
        Boolean adv = false;
    }

    @GetMapping("/podcast/getepisode/{curuuid}/{nextuuid}")
    public myresponse GetRESTEpisode (
            @PathVariable String curuuid,
            @PathVariable String nextuuid,
            Model model) {
        logger.info("Поточний {} Беремо наступний епізод {}", curuuid, nextuuid);
        myresponse mr = new myresponse();
        PodcastItem ep = podcastService.GetEpisodeByUUID(nextuuid);
        if (ep != null) {
            mr.setCurrent(curuuid);
            mr.setNext(nextuuid);
            mr.setAdv(false);
            mr.setStoreuuid(ep.getEnclosurestore().getUuid());
            mr.setFile(ep.getEnclosurestore().getFilename());
            mr.setTitle(ep.getTitle());
        }

        return mr;
    }

    @PostMapping("/api/podcast/link-episode/{puuid}/{storeUUID}")
    public ResponseEntity<?> linkEpisode(
            @PathVariable String puuid,
            @PathVariable String storeUUID) {

        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) return ResponseEntity.status(401).body("Unauthorized");

        PodcastChannel podcast = podcastService.GetChanelByUUID(puuid);
        if (podcast == null) return ResponseEntity.status(404).body("Podcast not found");

        Store store = podcastService.GetStoreByUUID(storeUUID);
        if (store == null) return ResponseEntity.status(404).body("Store item not found");

        try {
            PodcastItem episode = new PodcastItem();
            episode.setChanel(podcast);
            episode.setStoreuuid(storeUUID);
            episode.setEnclosurestore(store);
            episode.setClientdetail(cd.getUuid());
            episode.setTimetrack(podcastService.GetTimeTrack(storeUUID));
            podcast.getItem().add(episode);

            podcastService.SavePodcast(podcast);

            return ResponseEntity.ok(Map.of("success", true, "episodeUuid", episode.getUuid()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
