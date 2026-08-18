package media.toloka.rfa.radio.guest;

import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.model.Album;
import media.toloka.rfa.radio.model.Track;
import media.toloka.rfa.radio.store.model.Store;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Slf4j
@Profile("Front")
@Controller
public class GuestAlbumController {

    @Autowired
    private CreaterService createrService;

    /**
     * Сторінка списку опублікованих альбомів з пагінацією.
     * GET /albums?page=0&size=12
     */
    @GetMapping("/albums")
    public String listAlbums(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "12") int size,
            Model model) {

        Page<Album> albumPage = createrService.GetPublishedAlbumsPage(page, size);
        model.addAttribute("albumPage", albumPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", albumPage.getTotalPages());

        return "/guest/albums";
    }

    /**
     * Публічна сторінка перегляду конкретного альбому.
     * GET /album/view/{uuid}
     */
    @GetMapping("/album/view/{uuid}")
    public String viewAlbum(@PathVariable String uuid, Model model) {
        Album album = createrService.GetPublishedAlbumByUuid(uuid);

        if (album == null) {
            log.warn("Публічний перегляд: Альбом з UUID {} не знайдено або не опублікований", uuid);
            return "redirect:/albums";
        }

        Store coverStore = null;
        if (album.getStoreuuidalbumcover() != null) {
            coverStore = createrService.GetStoreAlbumCoverByUUID(album.getStoreuuidalbumcover());
        }

        List<Track> tracks = album.getTrack();

        model.addAttribute("album", album);
        model.addAttribute("trackList", tracks);
        model.addAttribute("coverStore", coverStore);

        log.info("Відкрито публічний перегляд альбому: {}", album.getName());
        return "/guest/guest_album_view";
    }
}