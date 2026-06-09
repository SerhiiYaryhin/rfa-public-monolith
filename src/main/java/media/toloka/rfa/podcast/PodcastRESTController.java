package media.toloka.rfa.podcast;

import media.toloka.rfa.podcast.model.PodcastItem;
import media.toloka.rfa.podcast.service.PodcastService;
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

    @PostMapping("/episode/{uuid}/inc-looked")
    public ResponseEntity<?> incrementEpisodeLooked(@PathVariable String uuid) {
        PodcastItem item = podcastService.GetEpisodeByUUID(uuid);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        item.setLooked(item.getLooked() + 1L);
        podcastService.SaveEpisode(item);
        return ResponseEntity.ok(Map.of("success", true, "looked", item.getLooked()));
    }
}
