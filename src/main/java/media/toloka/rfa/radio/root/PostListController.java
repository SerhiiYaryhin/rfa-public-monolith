package media.toloka.rfa.radio.root;


import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.model.Post;
import media.toloka.rfa.radio.model.Track;
import media.toloka.rfa.radio.store.model.Store;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
@Profile("Front")
@Controller
public class PostListController {

    @Autowired
    private ClientService clientService;
    @Autowired
    private CreaterService createrService;

    @GetMapping(value = "/guest/postall/{page}")
    public String getTracksAll(
            @PathVariable int page,
//            @PathVariable String fileName,
            Model model) {
        List<Post> posts = createrService.GetAllPostsByApruveAndMusicPost(true);
        List<Track> trackList = createrService.GetLastUploadTracks();

        Page pageTrack = createrService.GetTrackPage(0,10);
        List<Store> storeTrackList = pageTrack.stream().toList();

//        Page page = storeService.GetStorePageItemType(0,5, STORE_TRACK);
        Page pagePost = createrService.GetPostPage(page,12);
        List<Store> storePostsList = pagePost.stream().toList();

        //        model.addAttribute("trackList", trackList );
        int privpage ;
        int nextpage ;
        if (page == 0) {privpage = 0;} else {privpage = page - 1;};
        if (page >= (pagePost.getTotalPages()-1) ) {nextpage = pagePost.getTotalPages()-1; } else {nextpage = page+1;} ;
        model.addAttribute("nextpagepost", nextpage );
        model.addAttribute("privpagepost", privpage );
        model.addAttribute("totalpagepost", pagePost.getTotalPages() );
        model.addAttribute("pagepost", pagePost );
        model.addAttribute("currentpage", page );
        model.addAttribute("postList", storePostsList );
        model.addAttribute("trackList", storeTrackList );
        model.addAttribute("posts", posts );
//        model.addAttribute("stations",  stationService.GetListStationByUser(user));

        return "/guest/postall";




    }
}
