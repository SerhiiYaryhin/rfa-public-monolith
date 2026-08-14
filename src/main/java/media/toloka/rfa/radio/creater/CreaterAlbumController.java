package media.toloka.rfa.radio.creater;

import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.model.Album;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Track;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Profile("Front")
@Controller
public class CreaterAlbumController {
    final Logger logger = LoggerFactory.getLogger(CreaterAlbumController.class);

    @Autowired
    private CreaterService createrService;
    @Autowired
    private ClientService clientService;
    @Autowired
    private media.toloka.rfa.radio.store.Service.StoreCleanupService cleanupService;

    private Clientdetail getCurrentClientdetail() {
        Users user = clientService.GetCurrentUser();
        return (user != null) ? clientService.GetClientDetailByUser(user) : null;
    }

    @PostMapping("/creater/setalbumcover/{storealcouuid}/{albumuuid}/{cdid}")
    public String GetAlbumSetCover(
            @PathVariable("storealcouuid") String storealcouuid,
            @PathVariable("albumuuid") String albumuuid,
            @PathVariable("cdid") Long cdid,
            Model model
    ) {
        Clientdetail cd = clientService.GetClientDetailById(cdid);
        if (cd == null) return "redirect:/";

        Album album = createrService.GetAlbumByUuid(albumuuid);
        if (album == null || !album.getClientdetail().getUuid().equals(cd.getUuid())) {
            return "redirect:/creater/albums";
        }

        Store store = createrService.GetStoreAlbumCoverByUUID(storealcouuid);
        if (store != null) {
            album.setStoreuuidalbumcover(store.getUuid());
            createrService.SaveAlbum(album);
        }

        List<Track> tracks = album.getTrack();
        String cover = album.getStoreuuidalbumcover();

        model.addAttribute("coverlist", createrService.GetAlbumCoverByCd(cd));
        model.addAttribute("cover", cover);
        model.addAttribute("album", album);
        model.addAttribute("trackList", tracks);
        return "/creater/editalbum";
    }

    @GetMapping(value = "/creater/albums")
    public String getCreaterAlbums(Model model) {
        Clientdetail cd = getCurrentClientdetail();
        if (cd == null) return "redirect:/";

        List<Album> albumList = createrService.GetAllAlbumsByCreater(cd);
        model.addAttribute("albumList", albumList);
        return "/creater/albums";
    }

    @GetMapping(value = "/creater/editalbum/{uuidAlbum}")
    public String getCreaterAlbumEdit(
            @PathVariable String uuidAlbum,
            @ModelAttribute Album falbum,
            Model model) {
        Clientdetail cd = getCurrentClientdetail();
        if (cd == null) return "redirect:/";

        Album album;
        if ("new".equals(uuidAlbum)) {
            album = new Album();
            album.setClientdetail(cd);
            album.setName("Новий");
            album.setAutor("-----------");
            createrService.SaveAlbum(album);
            // Редірект на реальний URL з UUID, щоб уникнути дублювання при оновленні сторінки (F5)
            return "redirect:/creater/editalbum/" + album.getUuid();
        } else {
            album = createrService.GetAlbumByUuid(uuidAlbum);
            if (album == null || !album.getClientdetail().getUuid().equals(cd.getUuid())) {
                return "redirect:/creater/albums";
            }
        }

        List<Track> tracks = album.getTrack();
        Store store;
        String cover;
        if (album.getStoreuuidalbumcover() != null) {
            store = createrService.GetStoreAlbumCoverByUUID(album.getStoreuuidalbumcover());
            cover = (store != null) ? store.getUuid() : null;
        } else {
            cover = null;
        }

        model.addAttribute("coverlist", createrService.GetAlbumCoverByCd(cd));
        model.addAttribute("cover", cover);
        model.addAttribute("album", album);
        model.addAttribute("trackList", tracks);
        return "/creater/editalbum";
    }

    @PostMapping(value = "/creater/editalbum")
    public String saveCreaterEditAlbum(
            @ModelAttribute Album falbum,
            Model model) {
        Clientdetail cd = getCurrentClientdetail();
        if (cd == null) return "redirect:/";

        // ВИПРАВЛЕННЯ: Шукаємо альбом саме за UUID, який прийшов з прихованого поля форми
        Album album = createrService.GetAlbumByUuid(falbum.getUuid());

        if (album == null || !album.getClientdetail().getUuid().equals(cd.getUuid())) {
            logger.warn("Спроба зберегти чужий або неіснуючий альбом з UUID={}", falbum.getUuid());
            return "redirect:/creater/albums";
        }

        album.setName(falbum.getName());
        // Тут коректно збережеться JSON з Editor.js
        album.setDescription(falbum.getDescription());
        album.setAlbumrelisedate(falbum.getAlbumrelisedate());
        album.setStyle(falbum.getStyle());
        album.setAutor(falbum.getAutor());
        album.setDatealbumrelise(falbum.getDatealbumrelise());

        createrService.SaveAlbum(album);
        return "redirect:/creater/albums";
    }

    @GetMapping(value = "/creater/setalbumcover/{uuidAlbum}")
    public String getCreaterAlbumsSetCover(
            @PathVariable String uuidAlbum,
            @ModelAttribute Album falbum,
            Model model) {
        Clientdetail cd = getCurrentClientdetail();
        if (cd == null) return "redirect:/";

        Album album = createrService.GetAlbumByUuid(uuidAlbum);
        if (album == null || !album.getClientdetail().getUuid().equals(cd.getUuid())) {
            return "redirect:/creater/albums";
        }

        Store store = createrService.GetStoreAlbumCoverByUUID(album.getStoreuuidalbumcover());
        String currentcover = (store != null) ? store.getFilename() : null;

        List<Store> coverlist = createrService.GetAlbumCoverByCd(cd);
        for (Store cover : coverlist) {
            logger.warn("Імʼя Файлу: {}", cover.getFilename());
        }

        model.addAttribute("coverlist", coverlist);
        model.addAttribute("cover", currentcover);
        model.addAttribute("album", album);
        model.addAttribute("cd", cd);
        return "/creater/setalbumcover";
    }

    @GetMapping(value = "/creater/deletealbum/{uuidAlbum}")
    public String deleteAlbum(@PathVariable String uuidAlbum, Model model) {
        Clientdetail cd = getCurrentClientdetail();
        if (cd == null) return "redirect:/";

        Album album = createrService.GetAlbumByUuid(uuidAlbum);
        if (album != null && album.getClientdetail().getUuid().equals(cd.getUuid())) {
            cleanupService.safeDeleteAlbum(album);
            logger.info("Успішно видалено альбом UUID={} користувачем={}", uuidAlbum, cd.getUuid());
        }
        return "redirect:/creater/albums";
    }
}