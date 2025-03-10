package media.toloka.rfa.radio.admin;

import media.toloka.rfa.radio.admin.service.AdminService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.security.model.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Profile("Front")
@Controller
public class AdminFinance {
    @Autowired
    private AdminService adminService;
    @Autowired
    private ClientService clientService;

    @GetMapping(value = "/admin/financeuser")
    public String getUserFinance(
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        return "/admin/financeusers";
    }
}
