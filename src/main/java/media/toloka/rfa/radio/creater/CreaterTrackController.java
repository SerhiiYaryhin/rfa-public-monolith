package media.toloka.rfa.radio.creater;


import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.document.ClientDocumentEditController;
import media.toloka.rfa.radio.dropfile.service.FilesService;
import media.toloka.rfa.radio.model.Album;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Track;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Profile("Front")
@Controller
public class CreaterTrackController {

    final Logger logger = LoggerFactory.getLogger(CreaterTrackController.class);

    @Autowired
    private ClientService clientService;

    @Autowired
    private FilesService filesService;
    @Autowired
    private CreaterService createrService;

    @Autowired
    private StoreService storeService;

    @GetMapping(value = "/creater/tracks")
    public String getCreaterTracks(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Integer curpage = page;
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        List<Store> storetrackList = createrService.storeListTrackByClientDetail(cd);
        List<Album> albums = createrService.GetAllAlbumsByCreater(cd);

        String baseaddress = filesService.GetBaseClientDirectory(cd);
//        model.addAttribute("baseaddress", baseaddress );


// Пейджинг для сторінки
//        Page pageStore = storeService.GetStorePageByClientDetail(curpage,10, cd);
        Page<Track> pageStore = createrService.GetTrackPageByClientDetail(curpage,10, cd);
        List<Track> treckList = pageStore.getContent();

        model.addAttribute("totalPages", pageStore.getTotalPages() );
        model.addAttribute("currentPage",curpage);
        model.addAttribute("linkPage","/creater/tracks");

        // Пейджинг для сторінки

        model.addAttribute("albums", albums );
        model.addAttribute("viewList", treckList );
//        model.addAttribute("storetrackList", storetrackList );
        return "/creater/tracks";
    }

    // Додаємо сумісність для старих посилань
    @GetMapping(value = "/creater/tracks/{cPage}")
    public String getCreaterTracksOld(@PathVariable String cPage) {
        return "redirect:/creater/tracks?page=" + cPage;
    }

    // /creater/edittrack/'+${track.id}
    @GetMapping(value = "/creater/edittrack/{uuidTrack}")
    public String getCreaterEditTracks(
            @PathVariable String uuidTrack,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());

        Track track = createrService.GetTrackByUuid(uuidTrack);
        logger.info("========= track.getUUID = {} track.getApruve = {} ",track.getUuid(),track.getApruve());

        Store store = storeService.GetStoreByUUID(track.getStoreitem().getUuid());

        List<Album> albumList = createrService.GetAllAlbumsByCreater(cd);

        model.addAttribute("albumList", albumList );
        model.addAttribute("track", track );
        model.addAttribute("store", store );

        return "/creater/edittrack";
    }

    @PostMapping(value = "/creater/edittrack")
    public String getCreaterEditTracks(
            @ModelAttribute("track") Track ftrack,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(user);
        Track track = null;
        
        // Використовуємо UUID як основний ключ пошуку
        if (ftrack.getUuid() != null && !ftrack.getUuid().isEmpty()) {
            track = createrService.GetTrackByUuid(ftrack.getUuid());
        }
        
        if (track == null) {
            logger.info("Не знайшли трек за UUID: {}", ftrack.getUuid());
            return "redirect:/creater/home";
        }
        
        // Оновлюємо поля
        if (ftrack.getName() != null) track.setName(ftrack.getName());
        if (ftrack.getAutor() != null) track.setAutor(ftrack.getAutor());
        if (ftrack.getDescription() != null) track.setDescription(ftrack.getDescription());
        if (ftrack.getStyle() != null) track.setStyle(ftrack.getStyle());
        
        track.setNotnormalvocabulary(ftrack.getNotnormalvocabulary());
        
        if (track.getTochat() != ftrack.getTochat()) {
            track.setTochat(ftrack.getTochat());
            createrService.PublicTrackToChat(track, cd );
        }
        
        if (ftrack.getAlbum() != null && ftrack.getAlbum().getId() != null && ftrack.getAlbum().getId() != 0) {
            track.setAlbum(ftrack.getAlbum());
        } else {
            track.setAlbum(null);
        }

        createrService.SaveTrack(track);

        return "redirect:/creater/tracks/0";
    }
// публікуємо трек
    @PostMapping(value = "/creater/publishtrack")
    public String getCreaterPublishTracks(
            @ModelAttribute Track ftrack,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(user);
        Track track = createrService.GetTrackById(ftrack.getId());
        if (track == null) {
            logger.info("З якогось дива не знайшли трек {}", ftrack.getId());
            return "/creater/home";
        }
        track.setName(ftrack.getName());
        track.setDescription(ftrack.getDescription());
        track.setNotnormalvocabulary(ftrack.getNotnormalvocabulary());
        track.setStyle(ftrack.getStyle());
        track.setAutor(ftrack.getAutor());
        if (track.getApruve() != true) {
        track.setApruve (true); //опублікували трек у переліку треків
        } else {
            track.setApruve (false);
        }
        if (track.getTochat() != ftrack.getTochat()) {
            track.setTochat(ftrack.getTochat());

            createrService.PublicTrackToChat(track, cd );
        }
        if (ftrack.getAlbum() != null) {
            track.setAlbum(ftrack.getAlbum());
        } else {
            track.setAlbum(null);
        }

        createrService.SaveTrack(track);

        List<Store> storetrackList = createrService.storeListTrackByClientDetail(cd);


        List<Track> trackList = createrService.GetAllTracksByCreater(cd);
//        model.addAttribute("trackList", trackList );
        model.addAttribute("storetrackList", storetrackList );

        // Пейджинг для сторінки
//        Page pageStore = storeService.GetStorePageByClientDetail(curpage,10, cd);
        //todo Зробити повернення на ту сторінку, з якої перейшли в редагування
        Integer curpage = 0;

        Page pageStore = createrService.GetTrackPageByClientDetail(curpage,10, cd);
        List<Store> treckList = pageStore.stream().toList();

        model.addAttribute("totalPages", pageStore.getTotalPages() );
        model.addAttribute("currentPage",curpage);
        model.addAttribute("linkPage","/creater/tracks/");

        // Пейджинг для сторінки

//        model.addAttribute("albums", albums );
        model.addAttribute("viewList", treckList );

        return "redirect:/creater/tracks/"+curpage.toString();
    }

}
