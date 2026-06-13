package media.toloka.rfa.radio.guest;

import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.model.Album;
import media.toloka.rfa.radio.model.Track;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Контролер для публічного перегляду альбомів слухачами.
 */
@Slf4j
@Profile("Front")
@Controller
public class GuestAlbumController {

    @Autowired
    private CreaterService createrService;

    /**
     * Публічна сторінка альбому.
     * GET /album/view/{uuid}
     */
    @GetMapping("/album/view/{uuid}")
    public String viewAlbum(@PathVariable String uuid, Model model) {
        Album album = createrService.GetAlbumByUuid(uuid);
        
        if (album == null) {
            log.warn("Публічний перегляд: Альбом з UUID {} не знайдено", uuid);
            return "redirect:/";
        }

        List<Track> tracks = album.getTrack();
        
        model.addAttribute("album", album);
        model.addAttribute("trackList", tracks);
        
        log.info("Відкрито публічний перегляд альбому: {}", album.getName());
        
        return "/guest/album_view";
    }
}
