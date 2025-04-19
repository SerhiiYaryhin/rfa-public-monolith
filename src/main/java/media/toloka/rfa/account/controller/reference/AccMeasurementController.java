package media.toloka.rfa.account.controller.reference;


import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.account.model.referens.AccMeasurementReference;
import media.toloka.rfa.account.sevice.reference.AccMeasurementService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.security.model.ERole;
import media.toloka.rfa.security.model.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/acc/reference/measurement")
public class AccMeasurementController {

    private final AccMeasurementService measurmentService;

    public AccMeasurementController(AccMeasurementService measurmentService) {
        this.measurmentService = measurmentService;
    }

    @Autowired
    private ClientService clientService;

    @GetMapping("/list")
    public String listGoods(Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) {
            return "redirect:/";
        }
        // перевірили права для роботи з планом рахунків
        // Admin та Cheef of Accaunts
        if (!(clientService.checkRole(user, ERole.ROLE_ADMIN) | clientService.checkRole(user, ERole.ROLE_ACCCHEAF))) {
            return "redirect:/";
        }

        model.addAttribute("measurement", measurmentService.FindAll());
//        /home/ysv/IdeaProjects/rfa/src/main/resources/templates/acc/reference/goods/list.html
        return "/acc/reference/measurement/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) {
            return "redirect:/";
        }
        // перевірили права для роботи з планом рахунків
        // Admin та Cheef of Accaunts
        if (!(clientService.checkRole(user, ERole.ROLE_ADMIN) | clientService.checkRole(user, ERole.ROLE_ACCCHEAF))) {
            return "redirect:/";
        }
        AccMeasurementReference measurementReference = new AccMeasurementReference();
        measurementReference.setOperator(cd);

        model.addAttribute("measurement", measurementReference);
        return "/acc/reference/measurement/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute AccMeasurementReference measurement) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) {
            return "redirect:/";
        }
        // перевірили права для роботи з планом рахунків
        // Admin та Cheef of Accaunts
        if (!(clientService.checkRole(user, ERole.ROLE_ADMIN) | clientService.checkRole(user, ERole.ROLE_ACCCHEAF))) {
            return "redirect:/";
        }

        measurement.setOperator(cd);
        try {
            measurmentService.Save(measurement);
        } catch (Exception e) {
            log.info("Save Товара не пройшов. {}",measurement);
        }
        return "redirect:/acc/reference/measurement/list";
    }

    @GetMapping("/edit/{uuid}")
    public String editForm(@PathVariable UUID uuid, Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) {
            return "redirect:/";
        }
        // перевірили права для роботи з планом рахунків
        // Admin та Cheef of Accaunts
        if (!(clientService.checkRole(user, ERole.ROLE_ADMIN) | clientService.checkRole(user, ERole.ROLE_ACCCHEAF))) {
            return "redirect:/";
        }

        var measurement = measurmentService.FindByUiid(uuid).orElseThrow();
        model.addAttribute("measurement", measurement);
        return "/acc/reference/measurement/form";
    }

    @GetMapping("/delete/{uuid}")
    public String delete(@PathVariable UUID uuid) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) {
            return "redirect:/";
        }
        // перевірили права для роботи з планом рахунків
        // Admin та Cheef of Accaunts
        if (!(clientService.checkRole(user, ERole.ROLE_ADMIN) | clientService.checkRole(user, ERole.ROLE_ACCCHEAF))) {
            return "redirect:/";
        }

        measurmentService.DeleteById(uuid);
        return "redirect:/acc/reference/measurement/list";
    }
}
