package media.toloka.rfa.accaunt.controller;

import media.toloka.rfa.accaunt.model.AccAccaunts;
import media.toloka.rfa.accaunt.service.AccService;
import media.toloka.rfa.blockeditor.model.BlockPost;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Track;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.security.model.ERole;
import media.toloka.rfa.security.model.Users;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class AccauntController {

    @Autowired
    private ClientService clientService;

    @Autowired
    private AccService accService;


    @GetMapping("/acc/acc/{pageNumber}")
    public String showForm(
            @PathVariable int pageNumber,
            @NotNull Model model) {
        // взяли поточного користувача
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        // перевірили права для роботи з планом рахунків
        // Admin та Cheef of Accaunts
        Boolean ttt = clientService.checkRole(user, ERole.ROLE_ADMIN);
        Boolean rrr = clientService.checkRole(user, ERole.ROLE_ACCCHEAF);
        Boolean eee = ttt | rrr;
//        if (    !clientService.checkRole(user, ERole.ROLE_ADMIN) | !clientService.checkRole(user, ERole.ROLE_ACCCHEAF)) {
        if ( !eee) {
            return "redirect:/";
        }

        Clientdetail operatorcd = clientService.GetClientDetailByUser(user);

        Page pageStore = accService.GetPage(pageNumber,10);
        List<AccAccaunts> storeList = pageStore.stream().toList();

//        model.addAttribute("trackList", trackList );
        int privpage ;
        int nextpage ;
        if (pageNumber == 0) {privpage = 0;} else {privpage = pageNumber - 1;};
        if (pageNumber >= (pageStore.getTotalPages()-1) ) {nextpage = pageStore.getTotalPages()-1; } else {nextpage = pageNumber+1;} ;
        // новий рядок навігації

        model.addAttribute("totalPages", pageStore.getTotalPages() );
        model.addAttribute("currentPage", pageNumber );
        model.addAttribute("viewList", storeList );
        model.addAttribute("pagetrack", pageStore );
        model.addAttribute("operatorcd", operatorcd);
        return "/acc/acc";
    }

    /// Редагування рахунку
    @GetMapping("/acc/editacc/{uuid}")
    public String GetFormEditAccount(
            @PathVariable String uuid,
            @NotNull Model model) {
        // взяли поточного користувача
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        // перевірили права для роботи з планом рахунків
        // Admin та CheefOfAccaunts
        if (!(clientService.checkRole(user, ERole.ROLE_ADMIN) | clientService.checkRole(user, ERole.ROLE_ACCCHEAF))) {
            return "/";
        }

        Clientdetail operatorcd = clientService.GetClientDetailByUser(user);
        AccAccaunts curacc = accService.GetAccAccauntByUUID(uuid);
        if (curacc == null) {
            // Записали інцендент до історії
            curacc = new AccAccaunts();
            curacc.setAcc(123L);
            curacc.setAccname("Name");
            curacc.setOperationcomment("=============================");
        }

        List<AccAccaunts> listAcc = accService.GetListAccaunts();

        model.addAttribute("listacc", listAcc);
        model.addAttribute("operatorcd", operatorcd);
        model.addAttribute("curacc", curacc);
        return "/acc/editacc";
    }

    /// Збереження змін в рахунку
//    @PostMapping("/acc/editacc/{uuid}")
    @PostMapping("/acc/editbox")
    public String PostFormEditAccount (
            @PathVariable String uuid,
            @ModelAttribute AccAccaunts acc,
            @NotNull Model model) {
        // взяли поточного користувача
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        // перевірили права для роботи з планом рахунків
        // Admin та Cheef of Accaunts
        if (!(clientService.checkRole(user, ERole.ROLE_ADMIN) | clientService.checkRole(user, ERole.ROLE_ACCCHEAF))) {
            return "/";
        }

        Clientdetail operatorcd = clientService.GetClientDetailByUser(user);
        List<AccAccaunts> listAcc = accService.GetListAccaunts();

        model.addAttribute("listacc", listAcc);
        model.addAttribute("operatorcd", operatorcd);
        return "/acc/acc";
    }

    /// Видалити рахунок
    @GetMapping("/acc/delacc/{uuid}")
    public String PostFormDelAccount (
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
        List<AccAccaunts> listAcc = accService.GetListAccaunts();

        model.addAttribute("listacc", listAcc);
        model.addAttribute("operatorcd", operatorcd);
        return "/acc/acc";
    }
}
