package media.toloka.rfa.radio.creater;


import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.dropfile.service.FilesService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Post;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Profile("Front")
@Controller
public class CreaterStoreController {

    final Logger logger = LoggerFactory.getLogger(CreaterStoreController.class);

    @Autowired
    private ClientService clientService;

    @Autowired
    private FilesService filesService;

    @Autowired
    private CreaterService createrService;

    @Autowired
    private StoreService storeService;

    @GetMapping(value = "/creater/store/{pageNumber}",
            produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_GIF_VALUE})
    public String getStoreList(
            @PathVariable int pageNumber,
//            @PathVariable String fileName,
//            @ModelAttribute Clientdetail fuserdetail,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Clientdetail cd = clientService.GetClientDetailByUser(user);

        Page pageStore = storeService.GetStorePageByClientDetail(pageNumber,10, cd);
        List<Store> storeList = pageStore.stream().toList();

//        model.addAttribute("trackList", trackList );
        int privpage ;
        int nextpage ;
        if (pageNumber == 0) {privpage = 0;} else {privpage = pageNumber - 1;};
        if (pageNumber >= (pageStore.getTotalPages()-1) ) {nextpage = pageStore.getTotalPages()-1; } else {nextpage = pageNumber+1;} ;
        // новий рядок навігації

        model.addAttribute("totalPages", pageStore.getTotalPages() );
        model.addAttribute("currentPage", pageNumber );
        model.addAttribute("linkPage", "/creater/store/");
        model.addAttribute("viewList", storeList );
        model.addAttribute("pagetrack", pageStore );

        return "/store/mainstore";
    }

    @GetMapping(value = "/creater/setpostmainpicture/{postUuid}/{pageNumber}")
    public String getStoreMainPictureForPost(
            @PathVariable String postUuid,
            @PathVariable int pageNumber,
//            @PathVariable String fileName,
//            @ModelAttribute Clientdetail fuserdetail,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Clientdetail cd = clientService.GetClientDetailByUser(user);
        Post curpost = createrService.GetPostByUuid(postUuid);

        Page pageStore = storeService.GetAllPictures(pageNumber,10, cd);
        List<Store> storeList = pageStore.stream().toList();

        int privpage ;
        int nextpage ;
        if (pageNumber == 0) {privpage = 0;} else {privpage = pageNumber - 1;};
        if (pageNumber >= (pageStore.getTotalPages()-1) ) {nextpage = pageStore.getTotalPages()-1; } else {nextpage = pageNumber+1;} ;

        model.addAttribute("curpost", curpost );
        model.addAttribute("nextpage", nextpage );
        model.addAttribute("privpage", privpage );
        model.addAttribute("totalpage", pageStore.getTotalPages() );
        model.addAttribute("pagetrack", pageStore );
        model.addAttribute("currentpage", pageNumber );
        model.addAttribute("storeList", storeList );

//        List<Store> storeList = storeService.GetAllPictures(cd);

        return "/creater/setpostmainpicture";
    }
}
