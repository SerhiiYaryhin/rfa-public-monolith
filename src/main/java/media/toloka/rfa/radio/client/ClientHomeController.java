package media.toloka.rfa.radio.client;

import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
//import media.toloka.rfa.radio.message.service.MessageService;
import media.toloka.rfa.radio.model.Post;
import media.toloka.rfa.radio.model.Track;
import media.toloka.rfa.radio.post.service.PostService;
import media.toloka.rfa.radio.station.service.StationService;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@Profile("Front")
@Controller
public class ClientHomeController {

//    @Autowired
//    private UserRepository userRepo;

    @Autowired
    private ClientService clientService;

    @Autowired
    private StoreService storeService;

    @Autowired
    private StationService stationService;

    @Autowired
    private CreaterService createrService;

    @Autowired
    private PostService postService;

    @Autowired
    private media.toloka.rfa.podcast.service.PodcastService podcastService;

    final Logger logger = LoggerFactory.getLogger(ClientHomeController.class);

    @GetMapping(value = "/user/user_page")
    public String userHome(
            @RequestParam(defaultValue = "0") Integer postPage,
            @RequestParam(defaultValue = "0") Integer trackPage,
            @RequestParam(defaultValue = "0") Integer podcastPage,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        media.toloka.rfa.radio.model.Clientdetail cd = clientService.GetClientDetailByUser(user);

        // 1. Пости користувача
        Page<Post> posts = createrService.GetPostPageByClientDetail(postPage, 6, cd);
        model.addAttribute("postList", posts.getContent());
        model.addAttribute("postTotalPages", posts.getTotalPages());
        model.addAttribute("postCurrentPage", postPage);

        // 2. Треки користувача
        Page<Track> tracks = createrService.GetTrackPageByClientDetail(trackPage, 10, cd);
        model.addAttribute("trackList", tracks.getContent());
        model.addAttribute("trackTotalPages", tracks.getTotalPages());
        model.addAttribute("trackCurrentPage", trackPage);

        // 3. Подкасти користувача
        Page<media.toloka.rfa.podcast.model.PodcastChannel> podcasts = podcastService.GetPodcastPageByCd(cd, podcastPage, 6);
        model.addAttribute("podcastList", podcasts.getContent());
        model.addAttribute("podcastTotalPages", podcasts.getTotalPages());
        model.addAttribute("podcastCurrentPage", podcastPage);

        return "/user/user_page";
    }

    @GetMapping(value = "/user/home/documents")
    public String UserManageDocuments(
            @ModelAttribute Users user,
            Model model
    ) {
        Long usri = user.getId();
        return "redirect:/user/documents";
    }

    @GetMapping(value = "/user/home/managestations")
    public String UserManageStation(
//            @ModelAttribute Users user,
            Model model
    ) {
//        Long usri = user.getId();
        return "redirect:/user/stations";
    }

    @GetMapping(value = "/user/home/managecontract")
    public String UserManageContract(
            @ModelAttribute Users user,
            Model model
    ) {
        Long usri = user.getId();
        return "redirect:/user/contract";
    }

    @GetMapping(value = "/user/home/usergetinfo")
    public String UserGetInfo(
            @ModelAttribute Users user,
            Model model
    ) {
        Long usri = user.getId();
        return "redirect:/user/usereditinfo";
    }

}