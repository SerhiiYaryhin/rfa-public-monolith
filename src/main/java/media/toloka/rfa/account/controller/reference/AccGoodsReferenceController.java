package media.toloka.rfa.account.controller.reference;


import media.toloka.rfa.account.model.referens.AccGoodsReference;
import media.toloka.rfa.account.sevice.reference.AccGoodsReferenceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/acc/reference")
public class AccGoodsReferenceController {

    private final AccGoodsReferenceService goodsService;

    public AccGoodsReferenceController(AccGoodsReferenceService goodsService) {
        this.goodsService = goodsService;
    }

    @GetMapping
    public String listGoods(Model model) {
        model.addAttribute("goods", goodsService.findAll());
//        /home/ysv/IdeaProjects/rfa/src/main/resources/templates/acc/reference/goods/list.html
        return "/acc/reference/goods/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("goods", new AccGoodsReference());
        return "goods/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute AccGoodsReference goods) {
        goodsService.save(goods);
        return "redirect:/goods";
    }

    @GetMapping("/edit/{uuid}")
    public String editForm(@PathVariable UUID uuid, Model model) {
        var goods = goodsService.findByUiid(uuid).orElseThrow();
        model.addAttribute("goods", goods);
        return "goods/form";
    }

    @GetMapping("/delete/{uuid}")
    public String delete(@PathVariable UUID uuid) {
        goodsService.deleteById(uuid);
        return "redirect:/goods";
    }
}
