package media.toloka.rfa.author.controller;

import media.toloka.rfa.author.model.AuthorArticle;
import media.toloka.rfa.author.model.AuthorColumn;
import media.toloka.rfa.author.service.AuthorArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/guest/authors")
public class AuthorPublicController {

    @Autowired
    private AuthorArticleService articleService;

    @GetMapping
    public String authorHub(Model model) {
        model.addAttribute("columns", articleService.getAllColumns());
        return "/guest/author_hub";
    }

    @GetMapping("/{columnSlug}")
    public String authorColumn(@PathVariable String columnSlug, 
                               @RequestParam(defaultValue = "0") int page, 
                               Model model) {
        AuthorColumn column = articleService.getColumnBySlug(columnSlug)
                .orElseThrow(() -> new RuntimeException("Колонку не знайдено"));
        
        Page<AuthorArticle> articles = articleService.getPublishedArticlesByColumn(column, page, 12);
        
        model.addAttribute("column", column);
        model.addAttribute("articles", articles.getContent());
        model.addAttribute("totalPages", articles.getTotalPages());
        model.addAttribute("currentPage", page);
        
        return "/guest/author_column";
    }

    @GetMapping("/article/{articleSlug}")
    public String viewArticle(@PathVariable String articleSlug, Model model) {
        AuthorArticle article = articleService.getPublishedArticleBySlug(articleSlug)
                .orElseThrow(() -> new RuntimeException("Статтю не знайдено"));
        
        // Збільшуємо лічильник переглядів
        article.setLooked(article.getLooked() + 1);
        articleService.saveArticle(article);
        
        model.addAttribute("article", article);
        return "/guest/author_article_view";
    }
}
