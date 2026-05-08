package media.toloka.rfa.podcast;

import media.toloka.rfa.podcast.model.PodcastChannel;
import media.toloka.rfa.podcast.service.PodcastService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Profile("Front")
@Controller
public class AdminPodcastController {

    @Autowired
    private PodcastService podcastService;

    @Autowired
    private ClientService clientService;

    final Logger logger = LoggerFactory.getLogger(AdminPodcastController.class);

    /** Список усіх подкастів для адміністратора */
    @GetMapping(value = "/admin/podcasts")
    public String getAdminPodcasts(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        Page<PodcastChannel> podcastPage = podcastService.GetAllPodcastsPage(page, 15);

        model.addAttribute("podcastList", podcastPage.getContent());
        model.addAttribute("totalPages", podcastPage.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("linkPage", "/admin/podcasts");

        return "/admin/podcasts";
    }

    @GetMapping(value = "/admin/podcasts/{page}")
    public String getAdminPodcastsOld(@PathVariable Integer page) {
        return "redirect:/admin/podcasts?page=" + page;
    }

    /** Схвалення/відхилення подкасту */
    @GetMapping(value = "/admin/togglepodcast/{puuid}")
    public String togglePodcastStatus(@PathVariable String puuid) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        PodcastChannel podcast = podcastService.GetChanelByUUID(puuid);
        if (podcast != null) {
            podcast.setApruve(!podcast.getApruve());
            podcastService.SavePodcast(podcast);
            logger.info("Admin toggled podcast approval for {}: {}", puuid, podcast.getApruve());
        }
        return "redirect:/admin/podcasts";
    }
}
