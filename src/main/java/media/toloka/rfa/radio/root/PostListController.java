package media.toloka.rfa.radio.root;


import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.model.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Profile("Front")
@Controller
public class PostListController {

    final Logger logger = LoggerFactory.getLogger(PostListController.class);

    @Autowired
    private ClientService clientService;
    @Autowired
    private CreaterService createrService;

    @GetMapping(value = {"/guest/postall", "/guest/postall/{page}"})
    public String getPostsAll(
            @PathVariable(required = false) Integer page,
            Model model) {
        
        int currentPage = (page == null) ? 0 : page;
        
        // Отримуємо публічні пости (музичні та загальні)
        Page<Post> pagePost = createrService.GetPublicPostsPage(currentPage, 12);

        int privpage = (currentPage == 0) ? 0 : currentPage - 1;
        int nextpage = (currentPage >= (pagePost.getTotalPages() - 1)) ? pagePost.getTotalPages() - 1 : currentPage + 1;

        model.addAttribute("nextpage", nextpage);
        model.addAttribute("privpage", privpage);
        model.addAttribute("totalpage", pagePost.getTotalPages());
        model.addAttribute("currentpage", currentPage);
        model.addAttribute("postList", pagePost.getContent());
        model.addAttribute("pagepost", pagePost);
        
        // Дані для сайдбару (останні треки)
        model.addAttribute("trackList", createrService.GetPublicTracksPage(0, 10).getContent());

        return "/guest/postall";
    }
}
