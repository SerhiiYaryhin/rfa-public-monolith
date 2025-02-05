package media.toloka.rfa.podcast.service;

import dev.stalla.model.Episode;
import lombok.Data;
import media.toloka.rfa.podcast.PodcastController;
import media.toloka.rfa.podcast.model.PodcastItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PodcastRESTController {
    @Autowired
    private PodcastService podcastService;

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
}
