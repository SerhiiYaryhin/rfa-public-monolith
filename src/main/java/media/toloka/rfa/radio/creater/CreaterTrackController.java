package media.toloka.rfa.radio.creater;


import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.dropfile.service.FilesService;
import media.toloka.rfa.radio.model.Album;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Track;
import media.toloka.rfa.radio.store.Service.StoreCleanupService;
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
import org.springframework.web.bind.annotation.*;

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

    @Autowired
    private StoreCleanupService cleanupService;

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
        List<Album> albums = createrService.GetAllAlbumsByCreater(cd);

        Page<Track> pageStore = createrService.GetTrackPageByClientDetail(curpage,10, cd);
        List<Track> treckList = pageStore.getContent();

        model.addAttribute("totalPages", pageStore.getTotalPages() );
        model.addAttribute("currentPage",curpage);
        model.addAttribute("linkPage","/creater/tracks");

        model.addAttribute("albums", albums );
        model.addAttribute("viewList", treckList );
        return "/creater/tracks";
    }

    @GetMapping(value = "/creater/tracks/{cPage}")
    public String getCreaterTracksOld(@PathVariable String cPage) {
        return "redirect:/creater/tracks?page=" + cPage;
    }

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
    public String saveCreaterEditTracks(
            @ModelAttribute("track") Track ftrack,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(user);
        Track track = null;
        
        String inputUuid = ftrack.getUuid();
        if (inputUuid != null && !inputUuid.isEmpty()) {
            // 1. Спочатку шукаємо за UUID самого треку
            track = createrService.GetTrackByUuid(inputUuid);
            
            // 2. Якщо не знайдено, можливо це UUID зі сховища (для нових треків)
            if (track == null) {
                track = createrService.GetTrackByStoreuuid(inputUuid);
            }
            
            // 3. Якщо все ще не знайдено, перевіряємо чи є такий файл у сховищі
            if (track == null) {
                Store store = storeService.GetStoreByUUID(inputUuid);
                if (store != null) {
                    // Перевірка прав на файл
                    if (store.getClientdetail().getUuid().equals(cd.getUuid())) {
                        track = createrService.SaveTrackUploadInfo(inputUuid, cd);
                        logger.info("Створено новий об'єкт Track для існуючого файлу Store: {}", inputUuid);
                    }
                }
            }
        }
        
        if (track == null) {
            logger.info("Не змогли ідентифікувати трек або файл за UUID: {}", inputUuid);
            return "redirect:/creater/home";
        }
        
        // Перевірка прав власності на трек
        if (!track.getClientdetail().getUuid().equals(cd.getUuid())) {
            logger.warn("User {} tried to modify track {} belonging to another user", user.getEmail(), track.getUuid());
            return "redirect:/creater/home";
        }
        
        // Валідація обов'язкових полів на бекенді
        if (ftrack.getAutor() == null || ftrack.getAutor().trim().isEmpty() ||
            ftrack.getName() == null || ftrack.getName().trim().isEmpty() ||
            ftrack.getDescription() == null || ftrack.getDescription().trim().isEmpty()) {
            
            logger.warn("User {} tried to save track without mandatory fields", user.getEmail());
            // Оскільки ми використовуємо redirect, для передачі помилки зазвичай потрібен RedirectAttributes
            // Але в даному контексті ми можемо просто повернути користувача назад
            return "redirect:/creator/trackupload";
        }

        // Оновлюємо поля
        if (ftrack.getName() != null) track.setName(ftrack.getName());
        if (ftrack.getAutor() != null) track.setAutor(ftrack.getAutor());
        if (ftrack.getDescription() != null) track.setDescription(ftrack.getDescription());
        if (ftrack.getStyle() != null) track.setStyle(ftrack.getStyle());
        
        track.setNotnormalvocabulary(ftrack.getNotnormalvocabulary());
        
        if (track.getTochat() != ftrack.getTochat()) {
            track.setTochat(ftrack.getTochat());
            if (track.getTochat()) {
                createrService.PublicTrackToChat(track, cd );
            }
        }
        
        if (ftrack.getAlbum() != null && ftrack.getAlbum().getId() != null && ftrack.getAlbum().getId() != 0) {
            track.setAlbum(ftrack.getAlbum());
        } else {
            track.setAlbum(null);
        }

        createrService.SaveTrack(track);

        return "redirect:/creater/tracks?page=0";
    }

    @GetMapping(value = "/creater/publishtrack/{uuidTrack}")
    public String togglePublishTrack(@PathVariable String uuidTrack) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";
        
        Clientdetail cd = clientService.GetClientDetailByUser(user);
        Track track = createrService.GetTrackByUuid(uuidTrack);
        
        if (track != null && track.getClientdetail().getUuid().equals(cd.getUuid())) {
            if (track.getApruve()) {
                track.setPublishstatus(!track.getPublishstatus());
            } else {
                track.setPublishstatus(false);
            }
            createrService.SaveTrack(track);
        }
        return "redirect:/creater/tracks";
    }

    @GetMapping(value = "/creater/deltrack/{uuidTrack}")
    public String deleteTrack(@PathVariable String uuidTrack) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";
        
        Clientdetail cd = clientService.GetClientDetailByUser(user);
        Track track = createrService.GetTrackByUuid(uuidTrack);
        
        if (track != null && track.getClientdetail().getUuid().equals(cd.getUuid())) {
            cleanupService.safeDeleteTrack(track);
        }
        return "redirect:/creater/tracks";
    }

    @PostMapping(value = "/creater/publishtrack")
    public String postCreaterPublishTracks(
            @ModelAttribute Track ftrack,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(user);
        Track track = createrService.GetTrackById(ftrack.getId());
        if (track == null) {
            return "redirect:/creater/home";
        }
        
        track.setName(ftrack.getName());
        track.setDescription(ftrack.getDescription());
        track.setNotnormalvocabulary(ftrack.getNotnormalvocabulary());
        track.setStyle(ftrack.getStyle());
        track.setAutor(ftrack.getAutor());
        
        track.setApruve(!track.getApruve());
        
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
        return "redirect:/creater/tracks/0";
    }
}
