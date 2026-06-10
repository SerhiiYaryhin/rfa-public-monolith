package media.toloka.rfa.radio.guest;

import media.toloka.rfa.podcast.model.PodcastItem;
import media.toloka.rfa.podcast.service.PodcastService;
import media.toloka.rfa.radio.model.Track;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/1.0/podcast")
public class PodcastRESTController {

    @Autowired
    private PodcastService podcastService;

    @PostMapping("/{uuid}/inc-looked")
    public ResponseEntity<?> incrementPodcastLooked(@PathVariable String uuid) {
        PodcastItem podcastItem = podcastService.GetEpisodeByUUID(uuid);

        if (podcastItem == null) {
            return ResponseEntity.notFound().build();
        }

        podcastService.incrementEpisodeLooked(podcastItem);
        return ResponseEntity.ok(Map.of("success", true, "looked", podcastItem.getLooked()));
    }
}
