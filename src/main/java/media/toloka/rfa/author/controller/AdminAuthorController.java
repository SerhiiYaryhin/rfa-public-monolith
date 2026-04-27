package media.toloka.rfa.author.controller;

import media.toloka.rfa.author.service.AuthorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/author")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuthorController {

    @Autowired
    private AuthorService authorService;

    @GetMapping("/requests")
    public String listRequests(Model model) {
        model.addAttribute("requests", authorService.getAllPendingRequests());
        return "/admin/author/requests";
    }

    @GetMapping("/requests/approve/{uuid}")
    public String approve(@PathVariable String uuid) {
        authorService.approveRequest(uuid);
        return "redirect:/admin/author/requests?success=approved";
    }

    @GetMapping("/requests/reject/{uuid}")
    public String reject(@PathVariable String uuid) {
        authorService.rejectRequest(uuid);
        return "redirect:/admin/author/requests?success=rejected";
    }
}
