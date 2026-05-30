package media.toloka.rfa.podcast.fileupload;


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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;


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
        // Малюємо список всіх епізодів всіх подкастів для вибору незайнятих з можливістю зміни подкасту
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) { return "redirect:/"; }

        List<PodcastItem> podcastAllItemList = podcastService.GetAllEpisodePaging(cd);


        model.addAttribute("podcastAllItemList",  podcastAllItemList);
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
        Clientdetail cd = clientService.GetClientDetailByUser(user);

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

        List<Store> storeList = podcastService.GetPicturesListByCd(cd);

        model.addAttribute("storeList",  storeList);
        model.addAttribute("podcast",  podcast);
        model.addAttribute("episode",  episode);
        return "/podcast/podcastcoverepisodeupload";
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
        Clientdetail cd = clientService.GetClientDetailByUser(user);

        PodcastChannel podcast = podcastService.GetChanelByUUID(puuid);
        if (podcast == null ) {
            model.addAttribute("warning",  "Йой! Щось пішло не так - ми не знайшли Ваш Подкаст.");
        }

        model.addAttribute("storeList",  podcastService.GetPicturesListByCd(cd));
        model.addAttribute("podcast",  podcast);
        return "/podcast/podcastcoverupload";
    }

    @GetMapping("/podcast/audioupload/{puuid}/{euuid}")
    public String PodcastDropGetAudioEpisode (
            @PathVariable String puuid,
            @PathVariable String euuid,
            Model model
    ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(user);

        PodcastChannel podcast = podcastService.GetChanelByUUID(puuid);
        PodcastItem episode = podcastService.GetEpisodeByUUID(euuid);
        
        if (podcast == null || episode == null) {
            model.addAttribute("warning", "Подкаст або епізод не знайдено.");
            return "redirect:/podcast/home";
        }

        model.addAttribute("podcast", podcast);
        model.addAttribute("episode", episode);
        return "/podcast/podcastaudioupload";
    }

}

