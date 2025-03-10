package media.toloka.rfa.radio.creater;


import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.login.service.TokenService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Post;
import media.toloka.rfa.radio.post.service.PostService;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.UUID;

@Profile("Front")
@Controller
public class CreaterInfo {

    @Autowired
    private ClientService clientService;

    @Autowired
    private TokenService serviceToken;

    @Autowired
    private CreaterService createrService;

    @Autowired
    private PostService postService;

    final Logger logger = LoggerFactory.getLogger(CreaterInfo.class);


    @GetMapping(value="/creater/createtelegramtoken/{cduuid}")
    public String getTelegramToken(
            @PathVariable String cduuid,
            Model model ) {
//        Users user = clientService.GetClientDetailByUuid(cduuid);
//        if (user == null) {
//            return "redirect:/creater/info";
//        }
//        Users user =
        Clientdetail cd = clientService.GetClientDetailByUuid(cduuid);
        Users user = cd.getUser();
        String token = UUID.randomUUID().toString();
        serviceToken.createVerificationToken(user, token);
        model.addAttribute("clientdetail", cd );
        return "redirect:/creater/info";
    }

    @GetMapping(value = "/creater/info")
    public String getUserInfo(
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Clientdetail cd = clientService.GetClientDetailByUser(user);
        model.addAttribute("clientdetail", cd );
        model.addAttribute("createrService",createrService);


        return "/creater/info";
    }

    @PostMapping(value="/creater/info")
    public String postCreaterEditPost(
//            @PathVariable Long idcd,
            @ModelAttribute Clientdetail fcd,
            Model model )

    {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(user);
        //
        cd.setComments(fcd.getComments());
        cd.setFirmname(fcd.getFirmname());
        cd.setCustname(fcd.getCustname());
        cd.setCustsurname(fcd.getCustsurname());

        clientService.SaveClientDetail(cd);

        model.addAttribute("clientdetail", cd );
        return "/creater/info";
    }

    // Музичний Бот e2f9b0e6-73b5-4fcf-b249-f1e82d42a689
    @GetMapping(value = "/creater/profile/{uuidCreater}")
    public String getProfile(
            @PathVariable String uuidCreater,
            Model model ) {

        Clientdetail cd = clientService.GetClientDetailByUuid(uuidCreater);
        // загальна інформація розміщується у верхньому рядку
        // вся інша інформація розміщується у трьох колонках нижче.

        // Альбоми
        model.addAttribute("albumList", cd.getAlbumList() );
        // Треки
        model.addAttribute("trackList", cd.getTrackList() );
        // Пости
        model.addAttribute("postList", cd.getPostList() );

        model.addAttribute("clientdetail", cd );



        return "/guest/profile";
    }

}
