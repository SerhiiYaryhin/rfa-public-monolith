package media.toloka.rfa.radio.moderator;

import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Post;
import media.toloka.rfa.radio.post.service.PostService;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ModeratorController {

    @Autowired
    private ClientService clientService;

    @Autowired
    private CreaterService createrService;

    @Autowired
    private PostService postService;

    final Logger logger = LoggerFactory.getLogger(ModeratorController.class);

    @GetMapping(value = "/moderator/home")
    public String getUserHome(
            Model model ) {
        Users user = clientService.getCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Clientdetail cd = clientService.getClientDetailByUser(clientService.getCurrentUser());
//        List<Post> posts = createrService.GetAllPostsByCreater(cd);
//        model.addAttribute("posts", posts );

        return "/moderator/home";
    }

}
