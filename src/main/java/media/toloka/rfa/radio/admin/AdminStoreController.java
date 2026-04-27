package media.toloka.rfa.radio.admin;

import media.toloka.rfa.radio.admin.service.AdminService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.history.service.HistoryService;
import media.toloka.rfa.radio.post.service.PostService;
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

import java.util.List;

@Profile("Fro   nt")
@Controller
public class AdminStoreController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private CreaterService createrService;

    @Autowired
    private PostService postService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private StoreService storeService;


    final Logger logger = LoggerFactory.getLogger(AdminStoreController.class);


    @GetMapping(value = {"/admin/storage", "/admin/storage/{pageNumber}"})
    public String getAdminStore(
            @PathVariable(required = false) Integer pageNumber,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        int currentPage = (pageNumber == null) ? 0 : pageNumber;
        Page<Store> pageStore = storeService.GetStorePage(currentPage, 20);

        model.addAttribute("storeList", pageStore.getContent());
        model.addAttribute("totalPages", pageStore.getTotalPages());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("linkPage", "/admin/storage/");
        
        return "/admin/storage";
    }
}
