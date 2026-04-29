package media.toloka.rfa.radio.root;

import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.model.Track;
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
    private CreaterService createrService;

    @GetMapping(value = {"/guest/tracksall", "/guest/tracksall/{page}"})
    public String getTracksAll(
            @PathVariable(required = false) Integer page,
            Model model) {
        
        int currentPage = (page == null) ? 0 : page;
        
        // Отримуємо лише схвалені та опубліковані треки
        Page<Track> pageTrack = createrService.GetPublicTracksPage(currentPage, 15);
        List<Track> publicTracks = pageTrack.getContent();

        int privpage = (currentPage == 0) ? 0 : currentPage - 1;
        int nextpage = (currentPage >= (pageTrack.getTotalPages() - 1)) ? Math.max(0, pageTrack.getTotalPages() - 1) : currentPage + 1;

        model.addAttribute("nextpage", nextpage);
        model.addAttribute("privpage", privpage);
        model.addAttribute("totalpage", pageTrack.getTotalPages());
        model.addAttribute("currentpage", currentPage);
        model.addAttribute("trackList", publicTracks);
        model.addAttribute("pagetrack", pageTrack);

        return "/guest/tracksall";
    }
}
