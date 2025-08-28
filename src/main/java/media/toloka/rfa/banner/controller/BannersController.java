package media.toloka.rfa.banner.controller;

import lombok.RequiredArgsConstructor;
import media.toloka.rfa.banner.model.Banner;
import media.toloka.rfa.banner.repositore.BannerRepository;
import media.toloka.rfa.banner.service.BannerService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.repository.ClientDetailRepository;
import media.toloka.rfa.security.model.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Optional;

@Controller
@RequestMapping("/banners")
@RequiredArgsConstructor
public class BannersController {

    @Autowired
    private BannerService bannerService;
//    private final ClientDetailRepository clientdetailRepository;
    @Autowired
    private ClientService clientService;

    /**
     * Список всіх банерів
     */
    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

//        Page<Banner> bannersPage = bannerRepository.findAll(PageRequest.of(page, size, Sort.by("id").descending()));
        Page<Banner> bannersPage = bannerService.BannerGetPage(page, size);

        model.addAttribute("bannersPage", bannersPage);
        model.addAttribute("banners", bannerService.BannerGetPage(page,size));
//        model.addAttribute("banners", bannerService.BannerGetAll());
        model.addAttribute("currentSize", size);
        return "/banner/banner-list"; // окремий шаблон для списку
    }

    /**
     * Форма створення нового банера
     */
    @GetMapping("/new")
    public String createForm(Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) {
            return "redirect:/";
        }

        Banner banner = new Banner();
        banner.setClientdetail(cd);
        prepareForm(model, banner);
        return "/banner/banner-form";
    }

    /**
     * Форма редагування існуючого банера
     */
    @GetMapping("/{uuid}/edit")
    public String editForm(@PathVariable String uuid, Model model) {
        Banner banner = bannerService.BannerGetByUUID(uuid);
        if (banner == null) {
            return "redirect:/banners";
        }
        prepareForm(model, banner);
        return "/banner/banner-form";
    }

    /**
     * Збереження банера (створення або редагування)
     */
    @PostMapping("/save")
    public String save(@ModelAttribute("banner") Banner banner,
                       BindingResult bindingResult,
                       Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) {
            return "redirect:/";
        }

        if (banner.getClientdetail() == null) banner.setClientdetail(cd);

        // перевіряємо валідацію
        if (bindingResult.hasErrors()) {
            prepareForm(model, banner);
            return "/banner/banner-form";
        }

        if (banner.getApprove() == false) {
            banner.setAprovedate(null);
        } else {
            if (banner.getAprovedate() == null) banner.setAprovedate(new Date());
        }

        bannerService.BannerSave (banner);
        return "redirect:/banners";
    }

    /// Видаляємо баннер
    @PostMapping("/{uuid}/delete")
    public String delete(@PathVariable String uuid) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) {
            return "redirect:/";
        }

        Banner banner = bannerService.BannerGetByUUID(uuid);
        if (banner != null) bannerService.BannerDelete(uuid);
        return "redirect:/banners";
    }
    /// Змінюємо статус і дату погодження
    @PostMapping("/{uuid}/toggle-approve")
    public String toggleApprove(@PathVariable String uuid) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) {
            return "redirect:/";
        }

        Banner banner = bannerService.BannerGetByUUID(uuid);
        if (banner != null) {
            // Змінюємо дату схвалення банеру
            if (banner.getAprovedate() == null) { banner.setAprovedate(new Date()); }
                    else banner.setAprovedate(null);
            banner.setApprove(!Boolean.TRUE.equals(banner.getApprove()));
            bannerService.BannerSave(banner);
        }
        return "redirect:/banners";
    }

    /// Хелпер для додавання об'єкта банера і списку клієнтів у модель
    private void prepareForm(Model model, Banner banner) {
//        List<Clientdetail> clients = clientdetailRepository.findAll();
        model.addAttribute("banner", banner);
//        model.addAttribute("clients", clients);
    }

}
