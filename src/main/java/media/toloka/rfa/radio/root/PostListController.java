package media.toloka.rfa.radio.root;


import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.model.Post;
import media.toloka.rfa.radio.model.Track;
import media.toloka.rfa.radio.store.model.Store;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
@Profile("Front")
@Controller
public class PostListController {

    @Autowired
    private ClientService clientService;
    @Autowired
    private CreaterService createrService;

    @GetMapping(value = {"/guest/postall", "/guest/postall/{page}"})
    public String getTracksAll(
            @PathVariable(required = false) Integer page,
            Model model) {
        
        int currentPage = (page == null) ? 0 : page;
        
        // Отримуємо лише публічні пости
        Page<Post> pagePost = createrService.GetPublicPostsPage(currentPage, 12);

        model.addAttribute("totalPages", pagePost.getTotalPages());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("linkPage", "/guest/postall/");
        model.addAttribute("postList", pagePost.getContent());
        model.addAttribute("pagepost", pagePost);
        
        // Додаткові дані для сайдбару (якщо потрібно)
        model.addAttribute("trackList", createrService.GetPublicTracksPage(0, 10).getContent());

        return "/guest/postall";
    }
}
