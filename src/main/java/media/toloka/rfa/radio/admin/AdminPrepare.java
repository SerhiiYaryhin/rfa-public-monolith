package media.toloka.rfa.radio.admin;

import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Documents;
import media.toloka.rfa.security.model.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Profile("Front")
@Controller
public class AdminPrepare {

    @Autowired
    private ClientService clientService;

    @GetMapping(value = "/admin/prepare/mp3")
    public String getUserHome(
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }


//        model.addAttribute("clientdetailList", clientdetailList );
        return "/admin/documents";
    }
}
