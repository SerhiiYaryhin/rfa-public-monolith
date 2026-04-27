package media.toloka.rfa.author.controller;

import media.toloka.rfa.author.model.AuthorArticle;
import media.toloka.rfa.author.service.AuthorArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/author/articles")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuthorArticleController {

    @Autowired
    private AuthorArticleService articleService;

    @GetMapping("/pending")
    public String listPending(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<AuthorArticle> articles = articleService.getArticlesForModeration(page, 12);
        model.addAttribute("articles", articles.getContent());
        model.addAttribute("totalPages", articles.getTotalPages());
        model.addAttribute("currentPage", page);
        return "/admin/author/articles_pending";
    }

    @GetMapping("/preview/{uuid}")
    public String preview(@PathVariable String uuid, Model model) {
        AuthorArticle article = articleService.getArticleByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Статтю не знайдено"));
        model.addAttribute("article", article);
        return "/admin/author/article_preview";
    }

    @PostMapping("/approve/{uuid}")
    public String approve(@PathVariable String uuid) {
        articleService.publishArticle(uuid);
        return "redirect:/admin/author/articles/pending?success=published";
    }

    @PostMapping("/reject/{uuid}")
    public String reject(@PathVariable String uuid) {
        articleService.rejectArticle(uuid);
        return "redirect:/admin/author/articles/pending?success=rejected";
    }
}
