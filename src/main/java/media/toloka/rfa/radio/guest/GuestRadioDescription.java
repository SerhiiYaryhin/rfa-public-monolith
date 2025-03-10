package media.toloka.rfa.radio.guest;


import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.radio.client.ClientHomeInfoController;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.radio.station.service.StationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Slf4j
@Profile("Front")
@Controller
public class GuestRadioDescription {
    // відображаємо радіостанцію.

    final Logger logger = LoggerFactory.getLogger(GuestRadioDescription.class);

    @Autowired
    private ClientService clientService;

    @Autowired
    private CreaterService createrService;

    @Autowired
    private StationService stationService;


    @GetMapping(value = "/guest/radio/{uuid}")
    public String guestRadioInfo(
            @PathVariable String uuid,
//            @ModelAttribute User user,
            Model model ) {
        // Відображаємо опис станції розклад та програвач
        Station station = stationService.GetStationByUUID(uuid);
        if (station == null) {
            // станцію не знайшли :(
            logger.info("Не знайшли станцію {}", uuid);
            return "redirect:/";
        }

        model.addAttribute("station",  station);

        return "/guest/guestviewstationinfo";
    }
}
