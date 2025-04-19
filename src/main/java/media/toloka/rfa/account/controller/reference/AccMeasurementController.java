package media.toloka.rfa.account.controller.reference;


import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.account.model.referens.AccMeasurementReference;
import media.toloka.rfa.account.sevice.reference.AccMeasurementService;
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

    @GetMapping("/list")
    public String listGoods(Model model) {
        model.addAttribute("measurement", measurmentService.FindAll());
//        /home/ysv/IdeaProjects/rfa/src/main/resources/templates/acc/reference/goods/list.html
        return "/acc/reference/measurement/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("measurement", new AccMeasurementReference());
        return "/acc/reference/measurement/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute AccMeasurementReference measurement) {
        try {
            measurmentService.Save(measurement);
        } catch (Exception e) {
            log.info("Save Товара не пройшов. {}",measurement);
        }
        return "redirect:/acc/reference/measurement/list";
    }

    @GetMapping("/edit/{uuid}")
    public String editForm(@PathVariable UUID uuid, Model model) {
        var measurement = measurmentService.FindByUiid(uuid).orElseThrow();
        model.addAttribute("measurement", measurement);
        return "/acc/reference/measurement/form";
    }

    @GetMapping("/delete/{uuid}")
    public String delete(@PathVariable UUID uuid) {
        measurmentService.DeleteById(uuid);
        return "redirect:/acc/reference/measurement/list";
    }
}
