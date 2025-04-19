package media.toloka.rfa.account.controller.reference;


import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.account.model.referens.AccGoodsReference;
import media.toloka.rfa.account.model.referens.AccMeasurementReference;
import media.toloka.rfa.account.sevice.reference.AccGoodsService;
import media.toloka.rfa.account.sevice.reference.AccMeasurementService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @GetMapping("/list")
    public String listGoods(Model model) {
        List<AccGoodsReference> goodsReferenceList = goodsService.FindAll();
        model.addAttribute("goods", goodsReferenceList);
//        /home/ysv/IdeaProjects/rfa/src/main/resources/templates/acc/reference/goods/list.html
        return "/acc/reference/goods/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        List<AccMeasurementReference> measurementReferenceList = measurementService.FindAll();
        model.addAttribute("goods", new AccGoodsReference());
        model.addAttribute("measurementList", measurementReferenceList);
        return "/acc/reference/goods/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute AccGoodsReference goods) {
        try {
            log.info("{}",goods.getUuid());
            goodsService.Save(goods);
        } catch (Exception e) {
            log.info("Save Товара не пройшов. {}",goods);
        }
//        goodsService.save(goods);
        return "redirect:/acc/reference/goods/list";
    }

    @GetMapping("/edit/{uuid}")
    public String editForm(@PathVariable UUID uuid, Model model) {
        var goods = goodsService.FindByUiid(uuid).orElseThrow();

        model.addAttribute("measurementList", measurementService.FindAll());
        model.addAttribute("goods", goods);
        return "/acc/reference/goods/form";
    }

    @GetMapping("/delete/{uuid}")
    public String delete(@PathVariable UUID uuid) {
        goodsService.DeleteById(uuid);
        return "redirect:/acc/reference/goods/list";
    }
}
