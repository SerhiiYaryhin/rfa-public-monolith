package media.toloka.rfa.podcast;


import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.podcast.model.PodcastChannel;
//import media.toloka.rfa.podcast.model.PodcastImage;
import media.toloka.rfa.podcast.model.PodcastItem;
import media.toloka.rfa.podcast.service.PodcastService;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Profile("Front")
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
        if (tepisode != null) {
            // Заповнюємо поля знайденого епізоду з форми.
            tepisode.setTitle(episode.getTitle());
            tepisode.setDescription(episode.getDescription());

            podcastService.SaveEpisode(tepisode);
        }
        PodcastChannel podcast = tepisode.getChanel();
//        List<PodcastItem> itemList = podcast.getItem();

        model.addAttribute("podcast",  podcast);
//        model.addAttribute("itemslist",  itemList);
        return "redirect:/podcast/pedit/"+podcast.getUuid();
    }

    // Зберігаємо обкладинку для епізоду з форми завантаження та вибору обкладинки.
    @GetMapping(value = "/podcast/coverepisodeset/{euuid}/{iuuid}")
    public String CoverEpisodeUpload(
            @PathVariable String euuid, // uuid епізоду
            @PathVariable String iuuid, // uuid обкладинки у сховищі
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        PodcastItem episode = podcastService.GetEpisodeByUUID(euuid);
        Store podcastImage = podcastService.GetStoreByUUID(iuuid);
        if (episode != null && podcastImage != null) {
            episode.setImagestoreitem(podcastImage);
            podcastService.SaveEpisode(episode); // Зберігаємо саме епізод спочатку
            
            logger.info("Призначили обкладинку {} для епізоду {}",
                    podcastImage.getUuid(), episode.getUuid());
        }

        return "redirect:/podcast/episodeedit/" + episode.getChanel().getUuid() + "/" + episode.getUuid();
    }

    /** Видалення епізоду подкасту */
    @GetMapping(value = "/podcast/episodedel/{puuid}/{euuid}")
    public String deleteEpisode(
            @PathVariable String puuid, 
            @PathVariable String euuid,
            @RequestParam(defaultValue = "false") boolean deleteFile) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        PodcastItem episode = podcastService.GetEpisodeByUUID(euuid);
        if (episode != null) {
            // Перевіряємо власність через канал (uuid користувача у каналі)
            PodcastChannel channel = episode.getChanel();
            if (channel != null) {
                // Видаляємо епізод з колекції каналу
                channel.getItem().remove(episode);
                podcastService.SavePodcast(channel);
            }
            // Видаляємо сам епізод (і опційно файл)
            podcastService.DeleteEpisode(episode, deleteFile);
            logger.info("Користувач видалив епізод: {}. Видалення файлу: {}", euuid, deleteFile);
        }
        return "redirect:/podcast/pedit/" + puuid;
    }
}
