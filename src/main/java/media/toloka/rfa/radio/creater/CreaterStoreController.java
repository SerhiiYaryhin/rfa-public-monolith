package media.toloka.rfa.radio.creater;


import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.dropfile.service.FilesService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Post;
import media.toloka.rfa.radio.store.Service.StoreCleanupService;
import media.toloka.rfa.radio.store.Service.StoreDependencyService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

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

    @Autowired
    private StoreCleanupService cleanupService;

    @Autowired
    private StoreDependencyService dependencyService;

    @GetMapping(value = {"/creater/storage", "/creater/store"})
    public String getStorage(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";
        
        Clientdetail cd = clientService.GetClientDetailByUser(user);

        Page<Store> pageStore = storeService.GetStorePageByClientDetail(page, 15, cd);
        
        model.addAttribute("totalPages", pageStore.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("linkPage", "/creater/storage");
        model.addAttribute("viewList", pageStore.getContent());
        model.addAttribute("pagetrack", pageStore);

        return "/store/mainstore";
    }

    @GetMapping(value = {"/creater/storage/{pageNumber}", "/creater/store/{pageNumber}"})
    public String getStorageOld(@PathVariable Integer pageNumber) {
        return "redirect:/creater/storage?page=" + pageNumber;
    }

    @GetMapping(value = "/creater/store/delete/{uuid}")
    public String deleteStoreItem(
            @PathVariable String uuid,
            @RequestParam(required = false) String from,
            Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        Store item = storeService.GetStoreByUUID(uuid);
        if (item == null) return (from != null && from.equals("admin")) ? "redirect:/admin/storage" : "redirect:/creater/storage/0";

        // Перевірка прав
        if (!cleanupService.canUserDelete(item, user)) {
            logger.warn("User {} tried to delete store item {} without permission", user.getEmail(), uuid);
            return (from != null && from.equals("admin")) ? "redirect:/admin/storage" : "redirect:/creater/storage/0";
        }

        // Перевіряємо залежності
        Map<String, List<?>> deps = dependencyService.findDependencies(item);

        if (!deps.isEmpty()) {
            model.addAttribute("item", item);
            model.addAttribute("dependencies", deps);
            model.addAttribute("from", from);
            return "/store/confirm_delete";
        }

        // Якщо немає залежностей - видаляємо відразу
        cleanupService.performSafeDelete(item, false);
        logger.info("Видалено файл без залежностей: {}", uuid);

        return (from != null && from.equals("admin")) ? "redirect:/admin/storage" : "redirect:/creater/storage/0";
    }

    @PostMapping(value = "/creater/store/delete-confirmed")
    public String confirmDeleteStoreItem(
            @RequestParam String uuid,
            @RequestParam(required = false) String from,
            @RequestParam(defaultValue = "false") boolean onlyDetach) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        Store item = storeService.GetStoreByUUID(uuid);
        if (item != null) {
            // Перевірка прав
            if (!cleanupService.canUserDelete(item, user)) {
                return (from != null && from.equals("admin")) ? "redirect:/admin/storage" : "redirect:/creater/storage/0";
            }

            cleanupService.performSafeDelete(item, onlyDetach);
        }

        return (from != null && from.equals("admin")) ? "redirect:/admin/storage" : "redirect:/creater/storage/0";
    }

    @Autowired
    private media.toloka.rfa.radio.post.service.PostService postService;

    @GetMapping(value = "/creater/setpostmainpicture/{postUuid}/{pageNumber}")
    public String getStoreMainPictureForPost(
            @PathVariable String postUuid,
            @PathVariable int pageNumber,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Clientdetail cd = clientService.GetClientDetailByUser(user);
        
        Post curpost;
        if (postUuid.equals("0")) {
            curpost = new Post();
            curpost.setUuid("0");
            curpost.setClientdetail(cd);
        } else {
            curpost = postService.GetPostByUuid(postUuid);
            
            if (curpost == null) {
                logger.error("Пост з UUID {} не знайдено", postUuid);
                return "redirect:/creater/home";
            }
    
            if (!postService.canUserModifyPost(curpost, user)) {
                logger.warn("Користувач {} намагається отримати доступ до вибору ілюстрації поста {}, який йому не належить", user.getEmail(), postUuid);
                return "redirect:/creater/home";
            }
        }

        Page<Store> pageStore = storeService.GetAllPictures(pageNumber, 10, cd);
        List<Store> storeList = pageStore.getContent();

        int privpage = (pageNumber == 0) ? 0 : pageNumber - 1;
        int nextpage = (pageNumber >= (pageStore.getTotalPages() - 1)) ? pageStore.getTotalPages() - 1 : pageNumber + 1;

        model.addAttribute("curpost", curpost);
        model.addAttribute("nextpage", nextpage);
        model.addAttribute("privpage", privpage);
        model.addAttribute("totalpage", pageStore.getTotalPages());
        model.addAttribute("pagetrack", pageStore);
        model.addAttribute("currentpage", pageNumber);
        model.addAttribute("storeList", storeList);

        return "/creater/setpostmainpicture";
    }
}
