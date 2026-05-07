package media.toloka.rfa.author.controller;

import jakarta.servlet.http.HttpServletRequest;
import media.toloka.rfa.author.model.AuthorArticle;
import media.toloka.rfa.author.model.AuthorColumn;
import media.toloka.rfa.author.service.AuthorArticleService;
import media.toloka.rfa.banner.fileupload.BannerDropPostFileController;
import media.toloka.rfa.comments.model.enumerate.ECommentSourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    final Logger logger = LoggerFactory.getLogger(AuthorPublicController.class);

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
                              HttpServletRequest request,
                              Model model) {

        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && (userAgent.contains("facebookexternalhit") || userAgent.contains("Facebot"))) {
            logger.info("Facebook crawler accessing article: " + articleSlug);
            logger.info("User-Agent: " + userAgent);
            logger.info("Request URI: " + request.getRequestURI());
            logger.info("All headers: ");
            request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
                logger.info("  {}: {}", headerName, request.getHeader(headerName));
            });
        }

        AuthorArticle article = articleService.getPublishedArticleBySlug(articleSlug)
                .orElseThrow(() -> new RuntimeException("Статтю не знайдено"));

        // Збільшуємо лічильник переглядів
        article.setLooked(article.getLooked() + 1);
        articleService.saveArticle(article);

        // Завантажуємо коментарі
        var comments = commentService.getPaginatedCommentsHierarchy(ECommentSourceType.COMMENT_ARTICLE, article.getUuid(), org.springframework.data.domain.PageRequest.of(commentPage, 10));
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
