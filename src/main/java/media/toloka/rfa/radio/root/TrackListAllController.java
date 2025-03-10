package media.toloka.rfa.radio.root;


import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.store.model.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class TrackListAllController {

    final Logger logger = LoggerFactory.getLogger(TrackListAllController.class);

    @Autowired
    private ClientService clientService;
    @Autowired
    private CreaterService createrService;

    @GetMapping(value = "/guest/tracksall/{page}")
    public String getTracksAll(
            @PathVariable int page,
//            @PathVariable String fileName,
            Model model) {
//        List<Post> posts = createrService.GetAllPostsByApruve(true);
//        List<Track> trackList = createrService.GetLastUploadTracks();

//        Page page = storeService.GetStorePageItemType(0,5, STORE_TRACK);
        Page pageTrack = createrService.GetTrackPage(page,10);
        List<Store> storeTrackList = pageTrack.stream().toList();

//        model.addAttribute("trackList", trackList );
        int privpage ;
        int nextpage ;
        if (page == 0) {privpage = 0;} else {privpage = page - 1;};
        if (page >= (pageTrack.getTotalPages()-1) ) {nextpage = pageTrack.getTotalPages()-1; } else {nextpage = page+1;} ;
        model.addAttribute("nextpage", nextpage );
        model.addAttribute("privpage", privpage );
        model.addAttribute("totalpage", pageTrack.getTotalPages() );
        model.addAttribute("pagetrack", pageTrack );
        model.addAttribute("currentpage", page );
        model.addAttribute("trackList", storeTrackList );
//        model.addAttribute("posts", posts );
//        model.addAttribute("stations",  stationService.GetListStationByUser(user));

        return "/guest/tracksall";
    }

}
