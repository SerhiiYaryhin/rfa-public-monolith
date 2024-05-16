package media.toloka.rfa.radio.podcast;


import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.podcast.model.PodcastChannel;
import media.toloka.rfa.radio.podcast.model.PodcastImage;
import media.toloka.rfa.radio.podcast.model.PodcastItem;
import media.toloka.rfa.radio.podcast.service.PodcastService;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class EpisodeEditController {

    @Autowired
    private PodcastService podcastService;
    @Autowired
    private ClientService clientService;

    final Logger logger = LoggerFactory.getLogger(EpisodeEditController.class);

    @GetMapping(value = "/podcast/episodeedit/{puuid}/{euuid}")
    public String EpisodeRoot(
            @PathVariable String euuid,
            @PathVariable String puuid,
            Model model ) {
//        logger.info("Зайшли на епізод: /podcast/episodedit/{}",euuid);
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) { return "redirect:/"; }
        PodcastChannel podcast;

        PodcastItem episode = podcastService.GetEpisodeByUUID(euuid);
        podcast = podcastService.GetChanelByUUID(puuid);

        model.addAttribute("episode",  episode);
        model.addAttribute("podcast",  podcast);
        return "/podcast/episodeedit";
    }

    @PostMapping(value = "/podcast/episodesave/{euuid}")
    public String PodcastChanelSave (
            @PathVariable String euuid,
            @ModelAttribute PodcastItem episode,
//            @ModelAttribute Users formUserPSW,
            Model model ) {
        // Users user = clientService.GetCurrentUser();
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) { return "redirect:/"; }

        PodcastItem tepisode = podcastService.GetEpisodeByUUID(euuid);
        // Заповнюємо поля знайденого епізоду з форми.
        tepisode.setTitle(episode.getTitle());
        tepisode.setDescription(episode.getDescription());
        podcastService.SaveEpisode(tepisode);

        PodcastChannel podcast = tepisode.getChanel();
        List<PodcastItem> itemList = podcast.getItem();

        model.addAttribute("podcast",  podcast);
        model.addAttribute("itemslist",  itemList);
        return "redirect:/podcast/pedit/"+podcast.getUuid();
    }

    // Зберігаємо обкладинку для епізоду з форми завантаження та вибору обкладинки.
    // це посилання в шапці картинки для додавання до епізоду в формі завантаження/вибору обкладинки
    @GetMapping(value = "/podcast/coverepisodeupload/{euuid}/{iuuid}")
    public String CoverEpisodeUpload(
            @PathVariable String euuid,
            @PathVariable String iuuid,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) { return "redirect:/"; }

        PodcastItem episode = podcastService.GetEpisodeByUUID(euuid);
//        List<PodcastImage> podcastImageList =
        PodcastImage podcastImage = podcastService.GetImageByUUID(iuuid);
//        episode.setImage(podcastImage);

            // чистимо посилання на подкаст в епізоді та батьківському подкасті
            for (PodcastItem item : episode.getChanel().getItem()) {
                if (item.getId() == episode.getId() ) {
                    item.setImage(podcastImage);
                    break;
                }
            }
        podcastService.SavePodcast(episode.getChanel());

        model.addAttribute("episode",  episode);
        model.addAttribute("podcast",  episode.getChanel());
        return "redirect:/podcast/episodeedit/"+episode.getChanel().getUuid()+'/'+episode.getUuid();
    }


}
