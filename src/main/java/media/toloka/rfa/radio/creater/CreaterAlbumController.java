package media.toloka.rfa.radio.creater;

import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.model.Album;
import media.toloka.rfa.radio.model.Albumсover;
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

//@Profile("Front")
@Controller
public class CreaterAlbumController {

    final Logger logger = LoggerFactory.getLogger(CreaterAlbumController.class);

    @Autowired
    private CreaterService createrService;

    @Autowired
    private ClientService clientService;

    @PostMapping("/creater/setalbumcover/{storealcouuid}/{albumid}/{cdid}")
    public String GetAlbumSetCover(
//            @PathVariable Map<String, String> pathVarsMap
            // storealcouuid StoreAlbumCoverUUID
            @PathVariable("storealcouuid") String storealcouuid,
            @PathVariable("albumid") Long albumid,
            @PathVariable("cdid") Long cdid,
            Model model
    ) {
        // для сайту - запит та асінхронна обробка. https://www.cat-in-web.ru/fetch-async-await/

        Clientdetail cd = clientService.GetClientDetailById(cdid);
        Album album = createrService.GetAlbumById(albumid);
        Store store = createrService.GetStoreAlbumCoverByUUID(storealcouuid);
//        Albumсover albumсover = createrService.GetAlbumCoverById(alcoid);
        album.setStoreuuidalbumcover(store.getUuid());

        createrService.SaveAlbum(album);

        List<Track> tracks = album.getTrack();


        String albumсover = album.getStoreuuidalbumcover();
//        Store store;
        String cover;
        if (albumсover != null) {
            store = createrService.GetStoreAlbumCoverByUUID(storealcouuid);
            cover = store.getFilename();
        } else {
            cover = null;
        }

        model.addAttribute("coverlist", createrService.GetAlbumCoverByCd(cd) );
        model.addAttribute("cover", cover );
        model.addAttribute("album", album );
        model.addAttribute("trackList", tracks );
        return "/creater/editalbum";

//        Map<String,String> result;
//        return result;
    }

    @GetMapping(value = "/creater/albums")
    public String getCreaterAlbums(
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        List<Album> albumList = createrService.GetAllAlbumsByCreater(cd);
        model.addAttribute("albumList", albumList );
        return "/creater/albums";
    }

    @GetMapping(value = "/creater/editalbum/{idAlbum}")
    public String getCreaterAlbums(
            @PathVariable Long idAlbum,
            @ModelAttribute Album falbum,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Clientdetail cd = clientService.GetClientDetailByUser(user);
        Album album;
        if (idAlbum == 0L) {
            album = new Album();
            album.setClientdetail(cd);
            album.setName("Новий");
            album.setAutor("-----------");
            createrService.SaveAlbum(album);
        } else {
            album = createrService.GetAlbumById(idAlbum);
        }
        List<Track> tracks = album.getTrack();

        Store store;
        String cover;
        if (album.getStoreuuidalbumcover() != null) {
            store = createrService.GetStoreAlbumCoverByUUID(album.getStoreuuidalbumcover());
            cover = store.getUuid();
        } else {
            cover = null;
        }

        model.addAttribute("coverlist", createrService.GetAlbumCoverByCd(cd) );
        model.addAttribute("cover", cover );
        model.addAttribute("album", album );
        model.addAttribute("trackList", tracks );
        return "/creater/editalbum";
    }

    @PostMapping(value = "/creater/editalbum")
    public String getCreaterEditTracks(
//            @PathVariable Long idAlbum,
            @ModelAttribute Album falbum,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(user);
        Album album = createrService.GetAlbumById(falbum.getId());
        if (album == null) {
            logger.info("З якогось дива не знайшли альбом {}", falbum.getId());
            return "/creater/home";
        }
        album.setName(falbum.getName());
        album.setDescription(falbum.getDescription());
        album.setAlbumrelisedate(falbum.getAlbumrelisedate());
        album.setStyle(falbum.getStyle());
        album.setAutor(falbum.getAutor());
        album.setDatealbumrelise(falbum.getDatealbumrelise());
        if (album.getClientdetail() == null) { album.setClientdetail(cd); }

        createrService.SaveAlbum(album);

        List<Album> albumList = createrService.GetAllAlbumsByCreater(cd);
        model.addAttribute("albumList", albumList );
        return "/creater/albums";
    }

    @GetMapping(value = "/creater/setalbumcover/{idAlbum}")
    public String getCreaterAlbumsSetCover(
            @PathVariable Long idAlbum,
            @ModelAttribute Album falbum,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Clientdetail cd = clientService.GetClientDetailByUser(user);
        Album album;
        if (idAlbum == 0L) {
            album = new Album();
            album.setClientdetail(cd);
            createrService.SaveAlbum(album);
        } else {
            album = createrService.GetAlbumById(idAlbum);
        }

        //Albumсover albumсover = album.getAlbumcover();
        Store store = createrService.GetStoreAlbumCoverByUUID(album.getStoreuuidalbumcover());
        String currentcover;
        if (store != null) {

            currentcover = store.getFilename();
        } else {
            currentcover = null;
        }

        List<Albumсover> albumсover = createrService.GetAlbumCoverByCd(cd);
        for (Albumсover albumсover1 : albumсover) {
            if (albumсover1.getStoreitem() != null) {
                logger.warn("Імʼя Файлу: ", albumсover1.getStoreitem().getFilename());
            } else {
                logger.warn("Ще не призначили картинку");
            }
        }

        model.addAttribute("coverlist", albumсover );
        model.addAttribute("cover", currentcover );
        model.addAttribute("album", album );
        model.addAttribute("cd", cd );
        return "/creater/setalbumcover";
    }

}
