package media.toloka.rfa.radio.store;


import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.dropfile.service.FilesService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.store.model.EStoreFileType;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@CrossOrigin
@Controller
public class StoreItemController {

    final Logger logger = LoggerFactory.getLogger(StoreItemController.class);

    @Autowired
    private ClientService clientService;

    @Autowired
    private FilesService filesService;

    @Autowired
    private CreaterService createrService;

    @Autowired
    private StoreService storeService;

//    http://localhost:8080/creater/edititem/452
    @GetMapping(value = "/store/edititem/{storeItemUUID}")
    public String GetStoreItemEdit(
//    public @ResponseBody String GetStoreItemEdit(
        @PathVariable String storeItemUUID,
        Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) { return "redirect:/"; }

        Clientdetail cd = clientService.GetClientDetailByUser(user);

        Store store = storeService.GetStoreByUUID(storeItemUUID);

        List<EStoreFileType> storeFileList = new ArrayList<>(EnumSet.allOf(EStoreFileType.class));

        model.addAttribute("store", store );
        model.addAttribute("cd", cd );
        model.addAttribute("storefiletype", store.getStorefiletype() );
        model.addAttribute("storefilelist", storeFileList );

        return "/store/edititem";
    }

    @PostMapping(value = "/store/edititem/{storeItemUUID}")
    public String PostStoreItemEdit(
            @PathVariable String storeItemUUID,
            @ModelAttribute Store fstore,
            Model model) {

        Users user = clientService.GetCurrentUser();
        if (user == null) { return "redirect:/"; }

        Clientdetail cd = clientService.GetClientDetailByUser(user);
        Store store = storeService.GetStoreByUUID(storeItemUUID);
        store.setComment(fstore.getComment());
        store.setStorefiletype(fstore.getStorefiletype());
        storeService.SaveStore(store);

        return "redirect:/creater/store/0";

    }


    @PostMapping(value = "/store/deleteitem/{storeItemUUID}")
    public String PostStoreItemDelete(
            @PathVariable String storeItemUUID,
            Model model) {

        Users user = clientService.GetCurrentUser();
        if (user == null) { return "redirect:/"; }

        Clientdetail cd = clientService.GetClientDetailByUser(user);


//        model.addAttribute("store", store );
        return "redirect:/creater/store/0";

    }



}
