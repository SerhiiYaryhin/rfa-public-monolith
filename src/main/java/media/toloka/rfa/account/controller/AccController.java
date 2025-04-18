package media.toloka.rfa.account.controller;

import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.account.model.AccAccountsPlan;
import media.toloka.rfa.account.model.accEnum.EAccActivePassive;
import media.toloka.rfa.account.sevice.AccService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.newstoradio.model.ENewsVoice;
import media.toloka.rfa.security.model.ERole;
import media.toloka.rfa.security.model.Users;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;


@Slf4j
@RequestMapping("/acc")
@Controller
public class AccController {

    @Autowired
    private ClientService clientService;

    @Autowired
    private AccService accService;


    @GetMapping("/acc/{pageNumber}")
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
        if (!(clientService.checkRole(user, ERole.ROLE_ADMIN) | clientService.checkRole(user, ERole.ROLE_ACCCHEAF))) {
            return "redirect:/";
        }

        Clientdetail operatorcd = clientService.GetClientDetailByUser(user);

        Page pageStore = accService.GetPageAcc(pageNumber,10);
        List<AccAccountsPlan> storeList = pageStore.stream().toList();
        List<EAccActivePassive> eAccActivePassives = Arrays.asList(EAccActivePassive.values());

        model.addAttribute("totalPages", pageStore.getTotalPages() );
        model.addAttribute("currentPage", pageNumber );
        model.addAttribute("eAccActivePassives", eAccActivePassives );
        model.addAttribute("viewList", storeList );
        model.addAttribute("pagetrack", pageStore );
        model.addAttribute("operatorcd", operatorcd);
        return "/acc/acc";
    }

    /// Редагування рахунку
    @GetMapping("/editacc/{page}/{uuid}")
    public String GetFormEditAccount(
            @PathVariable String uuid,
            @PathVariable Integer page,
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
        AccAccountsPlan curacc;
        try {
            curacc = accService.GetAccAccountByUUID(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            log.info("Не спрацювало перетворення UUID. Переданий UUID: {}",uuid);
            // створюємо новий
            curacc = new AccAccountsPlan();
            if (curacc.getUuid() == null) {
                curacc.setUuid(UUID.randomUUID());
            }
//            if (curacc.getId() == null) {
//                curacc.setId(System.currentTimeMillis()); // Метод для генерації унікального ID
//            }
            curacc.setOperator( clientService.GetClientDetailByUser(clientService.GetCurrentUser()));
            curacc.setDocCreate(new Date());
//            return "redirect:/acc/acc/0";
        }

        List<AccAccountsPlan> listAcc = accService.GetListAccounts();
        List<EAccActivePassive> eAccActivePassives = Arrays.asList(EAccActivePassive.values());

        model.addAttribute("currentPage", page );
        model.addAttribute("listacc", listAcc);
        model.addAttribute("eAccActivePassives", eAccActivePassives);
        model.addAttribute("operatorcd", operatorcd);
        model.addAttribute("curacc", curacc);
        return "/acc/editacc";
    }

    /// Збереження змін в рахунку
    @PostMapping("/editbox/{pageNumber}/{uuid}")
    public String PostFormEditAccount (
            @PathVariable String uuid,
            @PathVariable Integer pageNumber,
            @ModelAttribute AccAccountsPlan acc,
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

        try {
            accService.Save(acc);
        } catch (DataIntegrityViolationException e) {
            // тут можна повернути повідомлення, що запис з такими даними вже існує
            model.addAttribute("danger", "Рахунок "+ acc.getAcc().toString()+" вже існує!");

        }

        Clientdetail operatorcd = clientService.GetClientDetailByUser(user);
        List<AccAccountsPlan> listAcc = accService.GetListAccounts();

        accService.GetListAccounts();

        Page pageStore = accService.GetPageAcc(pageNumber,10);
        List<AccAccountsPlan> storeList = pageStore.stream().toList();

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
        return "redirect:/acc/acc/"+pageNumber.toString();
    }

    /// Видалити рахунок
    @GetMapping("/delacc/{uuid}")
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
        if (!(clientService.checkRole(user, ERole.ROLE_ADMIN) | clientService.checkRole(user, ERole.ROLE_ACCCHEAF))) {
            return "redirect:/";
        }

        Clientdetail operatorcd = clientService.GetClientDetailByUser(user);

        AccAccountsPlan acc = accService.GetAccAccountByUUID(UUID.fromString(uuid));
        accService.DelAcc(acc);

        Page pageStore = accService.GetPageAcc(0,10);
        List<AccAccountsPlan> storeList = pageStore.stream().toList();

        model.addAttribute("totalPages", pageStore.getTotalPages() );
        model.addAttribute("currentPage", 0 );
        model.addAttribute("viewList", storeList );
        model.addAttribute("pagetrack", pageStore );
        model.addAttribute("operatorcd", operatorcd);
        return "redirect:/acc/acc/0";
    }
}
