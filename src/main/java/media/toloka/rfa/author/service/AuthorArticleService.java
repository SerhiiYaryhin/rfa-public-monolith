package media.toloka.rfa.author.service;

import media.toloka.rfa.author.model.AuthorArticle;
import media.toloka.rfa.author.model.AuthorColumn;
import media.toloka.rfa.author.model.enumerate.EAuthorArticleStatus;
import media.toloka.rfa.author.repository.AuthorArticleRepository;
import media.toloka.rfa.author.repository.AuthorColumnRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthorArticleService {

    @Autowired
    private AuthorArticleRepository articleRepository;

    @Autowired
    private AuthorColumnRepository columnRepository;

    public Page<AuthorArticle> getArticlesByColumn(AuthorColumn column, int page, int size) {
        return articleRepository.findByColumnAndStatus(column, null, PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    public Page<AuthorArticle> getAuthorArticles(AuthorColumn column, int page, int size) {
        return articleRepository.findByColumn(column, PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    public Optional<AuthorArticle> getArticleByUuid(String uuid) {
        return articleRepository.findByUuid(uuid);
    }

    @Transactional
    public AuthorArticle saveArticle(AuthorArticle article) {
        if (article.getSlug() == null || article.getSlug().isEmpty()) {
            article.setSlug(generateSlug(article.getTitle()));
        }
        return articleRepository.save(article);
    }

    private String generateSlug(String input) {
        return input.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-") + "-" + (System.currentTimeMillis() % 1000);
    }
}
