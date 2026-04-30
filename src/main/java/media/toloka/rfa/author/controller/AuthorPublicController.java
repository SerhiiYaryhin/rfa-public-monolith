package media.toloka.rfa.author.controller;

import media.toloka.rfa.author.model.AuthorArticle;
import media.toloka.rfa.author.model.AuthorColumn;
import media.toloka.rfa.author.service.AuthorArticleService;
import media.toloka.rfa.comments.model.enumerate.ECommentSourceType;
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

    @GetMapping("/articles-all")
    public String allArticles(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<AuthorArticle> articlesPage = articleService.getArticlesForMainPage(page, 16);
        model.addAttribute("articles", articlesPage.getContent());
        model.addAttribute("totalPages", articlesPage.getTotalPages());
        model.addAttribute("currentPage", page);
        return "/guest/author_articles_list";
    }

    @Autowired
    private media.toloka.rfa.comments.service.CommentService commentService;

    @GetMapping("/article/{articleSlug}")
    public String viewArticle(@PathVariable String articleSlug, 
                              @RequestParam(defaultValue = "0") int commentPage,
                              Model model) {
        AuthorArticle article = articleService.getPublishedArticleBySlug(articleSlug)
                .orElseThrow(() -> new RuntimeException("Статтю не знайдено"));
        
        // Збільшуємо лічильник переглядів
        article.setLooked(article.getLooked() + 1);
        articleService.saveArticle(article);
        
        // Завантажуємо коментарі
        var comments = commentService.getCommentsPage(article.getUuid(), commentPage, 10);
        model.addAttribute("commentsPage", comments);
        model.addAttribute("currentCommentsPage", commentPage);
        model.addAttribute("totalCommentsPages", comments.getTotalPages());
        model.addAttribute("totalComments", comments.getTotalElements());

        model.addAttribute("contentEntityType", ECommentSourceType.COMMENT_ARTICLE);
        model.addAttribute("contentEntityId", article.getUuid());
        model.addAttribute("contentAuthorId", article.getColumn().getAuthor().getUuid());
        model.addAttribute("currentUserId", commentService.getCurrentUserId() != null ? commentService.getCurrentUserId().getUuid() : null);

        model.addAttribute("article", article);
        return "/guest/author_article_view";
    }
}
