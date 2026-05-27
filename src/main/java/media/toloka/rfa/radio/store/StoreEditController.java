package media.toloka.rfa.radio.store;

import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.EStoreFileType;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.security.model.ERole;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Profile("Front")
@Controller
@RequestMapping("/store")
public class StoreEditController {

    private final Logger logger = LoggerFactory.getLogger(StoreEditController.class);

    @Autowired
    private StoreService storeService;

    @Autowired
    private ClientService clientService;

    @GetMapping("/edititem/{uuid}")
    public String editStoreItem(@PathVariable String uuid,
                               @RequestParam(required = false) String from,
                               Model model) {
        logger.info("Accessing editStoreItem for UUID: {}", uuid);
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            logger.info("User is null, redirecting to /");
            return "redirect:/";
        }

        Store store = storeService.GetStoreByUUID(uuid);
        if (store == null) {
            logger.warn("Store item not found for UUID: {}", uuid);
            return "redirect:/creater/storage/0";
        }

        // Перевірка прав: адмін може все, креатор тільки своє
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getRole().equals(ERole.ROLE_ADMIN));
        Clientdetail cd = clientService.GetClientDetailByUser(user);
        
        if (!isAdmin && !store.getClientdetail().getId().equals(cd.getId())) {
            logger.warn("User {} tried to edit store item {} without permission", user.getEmail(), uuid);
            return "redirect:/creater/storage/0";
        }

        model.addAttribute("store", store);
        model.addAttribute("from", from);
        model.addAttribute("storefilelist", EStoreFileType.values());
        
        logger.info("Successfully loaded edititem page for UUID: {}", uuid);
        return "/store/edititem";
    }

    @PostMapping("/edititem/{uuid}")
    public String saveStoreItem(@PathVariable String uuid,
                                @ModelAttribute Store storeData,
                                @RequestParam(required = false) String from) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        Store store = storeService.GetStoreByUUID(uuid);
        if (store == null) return "redirect:/creater/storage/0";

        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getRole().equals(ERole.ROLE_ADMIN));
        Clientdetail cd = clientService.GetClientDetailByUser(user);

        if (!isAdmin && !store.getClientdetail().getId().equals(cd.getId())) {
            return "redirect:/creater/storage/0";
        }

        store.setStorefiletype(storeData.getStorefiletype());
        store.setComment(storeData.getComment());
        
        storeService.SaveStore(store);
        logger.info("Store item {} updated by {}. New comment: {}", uuid, user.getEmail(), store.getComment());

        if ("admin".equals(from)) {
            return "redirect:/admin/storage";
        }
        return "redirect:/creater/storage/0";
    }
}
