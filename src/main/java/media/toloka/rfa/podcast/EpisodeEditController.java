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
            tepisode.setLead(episode.getLead());
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
        logger.info("[CONTROLLER] Запит на видалення епізоду: euuid={}, puuid={}, deleteFile={}", euuid, puuid, deleteFile);
        
        try {
            Users user = clientService.GetCurrentUser();
            if (user == null) {
                logger.warn("[CONTROLLER] Користувач не авторизований");
                return "redirect:/";
            }

            Clientdetail cd = clientService.GetClientDetailByUser(user);
            if (cd == null) {
                logger.warn("[CONTROLLER] Деталі клієнта не знайдені для користувача {}", user.getUsername());
                return "redirect:/";
            }

            PodcastItem episode = podcastService.GetEpisodeByUUID(euuid);
            if (episode != null) {
                PodcastChannel channel = episode.getChanel();
                if (channel != null && channel.getClientdetail().equals(cd.getUuid())) {
                    logger.info("[CONTROLLER] Авторизація успішна. Викликаємо сервіс видалення.");
                    podcastService.DeleteEpisode(episode, deleteFile);
                    logger.info("[CONTROLLER] Сервіс видалення повернув управління успішно.");
                } else {
                    logger.warn("[CONTROLLER] Спроба несанкціонованого видалення епізоду {} користувачем {}", euuid, cd.getUuid());
                    return "redirect:/podcast/home";
                }
            } else {
                logger.warn("[CONTROLLER] Епізод {} не знайдено в БД", euuid);
            }
            
            logger.info("[CONTROLLER] Виконуємо редірект на /podcast/pedit/{}", puuid);
            return "redirect:/podcast/pedit/" + puuid;
            
        } catch (Throwable t) {
            logger.error("[CONTROLLER FATAL] КРИТИЧНА ПОМИЛКА ПРИ ВИДАЛЕННІ: {}", t.getMessage(), t);
            // Виводимо повний стек у консоль (це вже робить logger.error з 't')
            return "redirect:/error"; 
        }
    }
}
