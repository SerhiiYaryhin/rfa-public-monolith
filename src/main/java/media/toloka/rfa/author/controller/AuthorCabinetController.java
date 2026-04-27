package media.toloka.rfa.author.controller;

import media.toloka.rfa.author.model.AuthorArticle;
import media.toloka.rfa.author.model.AuthorColumn;
import media.toloka.rfa.author.repository.AuthorColumnRepository;
import media.toloka.rfa.author.service.AuthorArticleService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.security.model.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/creater/author")
@PreAuthorize("hasRole('AUTHOR') or hasRole('ADMIN')")
public class AuthorCabinetController {

    @Autowired
    private AuthorArticleService articleService;

    @Autowired
    private AuthorColumnRepository columnRepository;

    @Autowired
    private ClientService clientService;

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(defaultValue = "0") int page, Model model) {
        Users user = clientService.GetCurrentUser();
        Clientdetail cd = clientService.GetClientDetailByUser(user);
        
        AuthorColumn column = columnRepository.findByAuthorUuid(cd.getUuid())
                .orElseThrow(() -> new RuntimeException("Колонку не знайдено"));

        Page<AuthorArticle> articles = articleService.getAuthorArticles(column, page, 10);

        model.addAttribute("column", column);
        model.addAttribute("articles", articles.getContent());
        model.addAttribute("totalPages", articles.getTotalPages());
        model.addAttribute("currentPage", page);
        
        return "/author/dashboard";
    }

    @GetMapping("/article/new")
    public String newArticle(Model model) {
        model.addAttribute("article", new AuthorArticle());
        return "/author/editor";
    }

    @GetMapping("/article/edit/{uuid}")
    public String editArticle(@PathVariable String uuid, Model model) {
        AuthorArticle article = articleService.getArticleByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Статтю не знайдено"));
        
        model.addAttribute("article", article);
        return "/author/editor";
    }
}
