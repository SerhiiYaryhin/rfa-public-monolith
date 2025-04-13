package media.toloka.rfa.accaunt.controller;

import media.toloka.rfa.accaunt.model.Accaunts;
import media.toloka.rfa.accaunt.service.AccService;
import media.toloka.rfa.blockeditor.model.BlockPost;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.security.model.ERole;
import media.toloka.rfa.security.model.Users;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class AccauntController {

    @Autowired
    private ClientService clientService;

    @Autowired
    private AccService accService;


    @GetMapping("/acc/accaunts")
    public String showForm(@NotNull Model model) {
        // взяли поточного користувача
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        // перевірили права для роботи з планом рахунків
        // Admin та Cheef of Accaunts
        if (    !clientService.checkRole(user, ERole.ROLE_ADMIN) | !clientService.checkRole(user, ERole.ROLE_ACCCHEAF)) {
            return "/";
        }

        Clientdetail operatorcd = clientService.GetClientDetailByUser(user);
        List<Accaunts> listAcc = accService.GetListAccaunts();

        model.addAttribute("listacc", listAcc);
        model.addAttribute("operatorcd", operatorcd);
        return "/acc/acchome";
    }

    /// Редагування рахунку
    @GetMapping("/acc/editaccount/{uuid}")
    public String GetFormEditAccount(
            @PathVariable String uuid,
            @NotNull Model model) {
        // взяли поточного користувача
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        // перевірили права для роботи з планом рахунків
        // Admin та Cheef of Accaunts
        if (    !clientService.checkRole(user, ERole.ROLE_ADMIN) | !clientService.checkRole(user, ERole.ROLE_ACCCHEAF)) {
            return "/";
        }

        Clientdetail operatorcd = clientService.GetClientDetailByUser(user);
        Accaunts curacc = accService.GetAccauntByUUID(uuid);
        if (curacc == null) {
            // Записали інцендент до історії
            return "/";
        }



        return "/acc/accaunts";
    }

    /// Збереження змін в рахунку
    @PostMapping("/acc/editaccount/{uuid}")
    public String PostFormEditAccount (
            @PathVariable String uuid,
            @NotNull Model model) {
        // взяли поточного користувача
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        // перевірили права для роботи з планом рахунків
        // Admin та Cheef of Accaunts
        if (    !clientService.checkRole(user, ERole.ROLE_ADMIN) | !clientService.checkRole(user, ERole.ROLE_ACCCHEAF)) {
            return "/";
        }

        Clientdetail operatorcd = clientService.GetClientDetailByUser(user);
        List<Accaunts> listAcc = accService.GetListAccaunts();

        model.addAttribute("listacc", listAcc);
        model.addAttribute("operatorcd", operatorcd);
        return "/acc/accaunts";
    }
}
