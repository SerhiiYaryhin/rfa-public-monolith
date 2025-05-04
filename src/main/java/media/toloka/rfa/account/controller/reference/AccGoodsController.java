package media.toloka.rfa.account.controller.reference;


import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.account.model.referens.AccGoodsReference;
import media.toloka.rfa.account.model.referens.AccMeasurementReference;
import media.toloka.rfa.account.sevice.reference.AccGoodsService;
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
@RequestMapping("/acc/reference/goods")
public class AccGoodsController {

    private final AccGoodsService goodsService;

    public AccGoodsController(AccGoodsService goodsService) {
        this.goodsService = goodsService;
    }

    @Autowired
    private AccMeasurementService measurementService;

    @Autowired
    private ClientService clientService;

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
        List<AccGoodsReference> goodsReferenceList = goodsService.FindAll();
        model.addAttribute("goods", goodsReferenceList);
//        /home/ysv/IdeaProjects/rfa/src/main/resources/templates/acc/reference/goods/list.html
        return "/acc/reference/goods/list";
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
        AccGoodsReference goodsReference = new AccGoodsReference();
        goodsReference.setOperator(cd);

        List<AccMeasurementReference> measurementReferenceList = measurementService.FindAll();
        model.addAttribute("goods", goodsReference);
        model.addAttribute("measurementList", measurementReferenceList);
        return "/acc/reference/goods/form";
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_Moderator')")
    @PostMapping("/save")
    public String save(@ModelAttribute AccGoodsReference goods) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) return "redirect:/";
        // перевірили права для роботи з планом рахунків
        // Admin та Cheef of Accaunts
        if (!(clientService.checkRole(user, ERole.ROLE_ADMIN) | clientService.checkRole(user, ERole.ROLE_ACCCHEAF))) {
            return "redirect:/";
        }
        goods.setOperator(cd);
        try {
            log.info("{}",goods.getUuid());
            goodsService.Save(goods);
        } catch (Exception e) {
            log.info("Save Товара не пройшов. {}",goods);
        }
//        goodsService.save(goods);
        return "redirect:/acc/reference/goods/list";
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_Moderator')")
    @GetMapping("/edit/{uuid}")
    public String editForm(@PathVariable UUID uuid, Model model) {
        var goods = goodsService.FindByUuid(uuid).orElseThrow();
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) return "redirect:/";
        // перевірили права для роботи з планом рахунків
        // Admin та Cheef of Accaunts
        if (!(clientService.checkRole(user, ERole.ROLE_ADMIN) | clientService.checkRole(user, ERole.ROLE_ACCCHEAF))) {
            return "redirect:/";
        }
        model.addAttribute("measurementList", measurementService.FindAll());
        model.addAttribute("goods", goods);
        return "/acc/reference/goods/form";
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
        goodsService.DeleteById(uuid);
        return "redirect:/acc/reference/goods/list";
    }
}
