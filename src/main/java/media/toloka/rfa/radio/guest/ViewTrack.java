package media.toloka.rfa.radio.guest;

import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.model.Track;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Profile("Front")
@Controller
public class ViewTrack {

    @Autowired
    private CreaterService createrService;

    // сайт світимо опис треку
    @GetMapping(value = "/guest/viewtrack/{trackuuid}")
    public String getTracksAll(
            @PathVariable String trackuuid,
            Model model) {
        Track curtrack = createrService.GetTrackByUuid(trackuuid);
        if (curtrack != null)   {
            // model.addAttribute("curtrack", curtrack );
            model.addAttribute("curtrack", curtrack );
            return "/guest/viewtrack";
        }
        return "redirect:/";
    }
}
