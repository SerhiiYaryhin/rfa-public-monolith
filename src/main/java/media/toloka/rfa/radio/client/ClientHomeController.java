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
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

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

    final Logger logger = LoggerFactory.getLogger(ClientHomeController.class);

    @GetMapping(value = "/user/user_page")
    public String userHome(
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        String curshow;
        // todo Взяти трек з List<ListOnlineFront> listOnlineFronts = StationOnlineList.getInstance().GetOnlineList();
        // та передати його на сайт

        // https://www.baeldung.com/java-read-json-from-url

//        List<Post> posts = createrService.GetAllPostsByApruveAndMusicPost(true);
//        List<Post> posts = createrService.GetAllPostsByApruveAndMusicPost(true);
//        List<Track> trackList = createrService.GetLastUploadTracks();

//        Page page = storeService.GetStorePageItemType(0,5, STORE_TRACK);
        Page pageTrack = createrService.GetTrackPage(0,10);
        List<Store> storeTrackList = pageTrack.stream().toList();

        model.addAttribute("trackList", storeTrackList );
        model.addAttribute("postList", createrService.GetAllPostsByApruveAndMusicPost(true) );

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