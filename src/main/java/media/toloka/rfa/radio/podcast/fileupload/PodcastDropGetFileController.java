package media.toloka.rfa.radio.podcast.fileupload;


import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.podcast.model.PodcastChannel;
import media.toloka.rfa.radio.podcast.model.PodcastItem;
import media.toloka.rfa.radio.podcast.service.PodcastService;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


//@Slf4j
//@RequestMapping("/creator/trackupload")
@Controller
public class PodcastDropGetFileController {

    @Autowired
    private ClientService clientService;

    @Autowired
    private PodcastService podcastService;

    @Value("${media.toloka.rfa.upload_directory}")
    private String PATHuploadDirectory;

    final Logger logger = LoggerFactory.getLogger(PodcastDropGetFileController.class);

    @GetMapping("/podcast/episodeupload/{puuid}")
    public String CreaterDropGetUploadTrack(
            @PathVariable String puuid,
            Model model
    ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        PodcastChannel podcast = podcastService.GetChanelByUUID(puuid);
        if (podcast == null ) {
            model.addAttribute("warning",  "Йой! Щось пішло не так - ми не знайшли Ваш Подкаст."
                    +" Зверніться будь ласка до служби підтримки");
        }
        model.addAttribute("podcast",  podcast);
        return "/podcast/podcastepisodeupload";
    }

    @GetMapping("/podcast/coverepisodeupload/{puuid}/{euuid}")
    public String PodcastDropGetCoverEpisode (
            @PathVariable String puuid,
            @PathVariable String euuid,
            Model model
    ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        PodcastChannel podcast = podcastService.GetChanelByUUID(puuid);
        if (podcast == null ) {
            model.addAttribute("warning",  "Йой! Щось пішло не так - ми не знайшли Ваш Подкаст."
                    +" Зверніться будь ласка до служби підтримки");
        }
        PodcastItem episode = podcastService.GetEpisodeByUUID(euuid);
        if (episode == null ) {
            model.addAttribute("warning",  "Йой! Щось пішло не так - ми не знайшли Ваш епізод."
                    +" Зверніться будь ласка до служби підтримки");
        }
        model.addAttribute("podcast",  podcast);
        model.addAttribute("episode",  episode);
        return "/podcast/coverepisodeupload";
    }

    @GetMapping("/podcast/coverpodcastupload/{puuid}")
    public String CreaterDropGetPicontroller (
            @PathVariable String puuid,
            Model model
    ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        PodcastChannel podcast = podcastService.GetChanelByUUID(puuid);
        if (podcast == null ) {
            model.addAttribute("warning",  "Йой! Щось пішло не так - ми не знайшли Ваш Подкаст."
                    +" Зверніться будь ласка до служби підтримки");
        }
        model.addAttribute("podcast",  podcast);
        return "/podcast/coverpodcastupload";
    }

}

