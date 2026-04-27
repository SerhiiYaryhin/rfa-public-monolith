package media.toloka.rfa.author.controller;

import media.toloka.rfa.author.model.AuthorRequest;
import media.toloka.rfa.author.service.AuthorService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.security.model.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/author")
public class AuthorController {

    @Autowired
    private AuthorService authorService;

    @Autowired
    private ClientService clientService;

    @GetMapping("/apply")
    public String showApplyForm(Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/login";

        model.addAttribute("authorRequest", new AuthorRequest());
        return "/author/apply";
    }

    @PostMapping("/apply")
    public String processApply(@ModelAttribute AuthorRequest authorRequest) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/login";

        Clientdetail cd = clientService.GetClientDetailByUser(user);
        authorRequest.setClient(cd);
        
        authorService.saveRequest(authorRequest);
        return "redirect:/user/profile?success=apply";
    }
}
