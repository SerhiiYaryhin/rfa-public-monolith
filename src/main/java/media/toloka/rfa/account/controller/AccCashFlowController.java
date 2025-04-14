package media.toloka.rfa.account.controller;

import media.toloka.rfa.account.model.dto.AccSummaryDto;
import media.toloka.rfa.account.service.AccService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.security.model.ERole;
import media.toloka.rfa.security.model.Users;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

public class AccCashFlowController {


    @Autowired
    private ClientService clientService;

    @Autowired
    private AccService accService;

    @GetMapping("/acc/operations/{pageNumber}")
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
            // не та роль
            return "redirect:/";
        }
//        Clientdetail cd = user.getClientdetail();
        Clientdetail operator = clientService.GetClientDetailByUser(user);

        List<AccSummaryDto> accCachFlowList = accService.getAccsWithSumOnMaxDate();

        model.addAttribute("viewList", accCachFlowList );
        model.addAttribute("operatorcd", operator);
        return "/acc/operations";
    }

}
