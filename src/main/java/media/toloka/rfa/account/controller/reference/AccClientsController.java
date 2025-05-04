package media.toloka.rfa.account.controller.reference;


import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.account.model.referens.AccClientsReference;
import media.toloka.rfa.account.sevice.reference.AccClientsService;
import media.toloka.rfa.account.sevice.reference.AccMeasurementService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.security.model.ERole;
import media.toloka.rfa.security.model.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/acc/reference/clients")
public class AccClientsController {

    @Autowired
    private AccMeasurementService measurementService;
//    @Autowired
//    private AccClientsService accClientsService1;
    @Autowired
    private ClientService clientService;
    @Autowired
    private AccClientsService  accClientsService;

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_Moderator')")
    @GetMapping("/list")
    public String listGoods(Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) return "redirect:/";
        // перевірили права для роботи з планом рахунків
        // Admin та Cheef of Accaunts
        if (!(clientService.checkRole(user, ERole.ROLE_ADMIN) | clientService.checkRole(user, ERole.ROLE_ACCCHEAF))) {
            return "redirect:/";
        }
        List<AccClientsReference> clientsReferences = accClientsService.FindAll();
        model.addAttribute("clientsList", clientsReferences);
        return "/acc/reference/clients/list";
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_Moderator')")
    @GetMapping("/create")
    public String createForm(Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) return "redirect:/";
        // перевірили права для роботи з планом рахунків
        // Admin та Cheef of Accaunts
        if (!(clientService.checkRole(user, ERole.ROLE_ADMIN) | clientService.checkRole(user, ERole.ROLE_ACCCHEAF))) {
            return "redirect:/";
        }
        AccClientsReference clientsReference = new AccClientsReference();
        clientsReference.setOperator(cd);
        List<Clientdetail> clientdetailList = clientService.GetAllClientDetail();
        model.addAttribute("client", clientsReference);
        model.addAttribute("cdList", clientdetailList);
        return "/acc/reference/clients/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute AccClientsReference client) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) return "redirect:/";
        // перевірили права для роботи з планом рахунків
        // Admin та Cheef of Accaunts
        if (!(clientService.checkRole(user, ERole.ROLE_ADMIN) | clientService.checkRole(user, ERole.ROLE_ACCCHEAF))) {
            return "redirect:/";
        }
        client.setOperator(cd);
        try {
            accClientsService.Save(client);
        } catch (Exception e) {
            log.info("Save Товара не пройшов. {}",client);
        }
        return "redirect:/acc/reference/clients/list";
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_Moderator')")
    @GetMapping("/edit/{uuid}")
    public String editForm(@PathVariable UUID uuid, Model model) {
        var client = accClientsService.FindByUuid(uuid).orElseThrow();
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";
        // перевірили права для роботи з планом рахунків
        // Admin та Cheef of Accaunts
        if (!(clientService.checkRole(user, ERole.ROLE_ADMIN) | clientService.checkRole(user, ERole.ROLE_ACCCHEAF))) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) return "redirect:/";
        model.addAttribute("client", client);
        return "/acc/reference/clients/form";
    }


    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_Moderator')")
    @GetMapping("/delete/{uuid}")
    public String delete(@PathVariable UUID uuid) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) return "redirect:/";
        // перевірили права для роботи з планом рахунків
        // Admin та Cheef of Accaunts
        if (!(clientService.checkRole(user, ERole.ROLE_ADMIN) | clientService.checkRole(user, ERole.ROLE_ACCCHEAF))) {
            return "redirect:/";
        }
        accClientsService.DeleteById(uuid);
        return "redirect:/acc/reference/clients/list";
    }
}
