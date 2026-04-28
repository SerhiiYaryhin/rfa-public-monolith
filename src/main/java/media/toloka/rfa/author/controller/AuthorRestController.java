package media.toloka.rfa.author.controller;

import media.toloka.rfa.author.model.AuthorArticle;
import media.toloka.rfa.author.model.AuthorColumn;
import media.toloka.rfa.author.repository.AuthorColumnRepository;
import media.toloka.rfa.author.service.AuthorArticleService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.Store;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static media.toloka.rfa.radio.store.model.EStoreFileType.STORE_POSTCOVER;

@RestController
@RequestMapping("/api/author")
public class AuthorRestController {

    @Autowired
    private AuthorArticleService articleService;

    @Autowired
    private AuthorColumnRepository columnRepository;

    @Autowired
    private ClientService clientService;

    @Autowired
    private StoreService storeService;

    @PostMapping("/article/save")
    public ResponseEntity<?> saveArticle(@RequestBody AuthorArticle articleData) {
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        AuthorColumn column = columnRepository.findByAuthorUuid(cd.getUuid())
                .orElseThrow(() -> new RuntimeException("Колонку не знайдено"));

        AuthorArticle article;
        // Шукаємо за UUID, який прийшов з фронтенду
        Optional<AuthorArticle> existingArticle = articleService.getArticleByUuid(articleData.getUuid());
        
        if (existingArticle.isPresent()) {
            article = existingArticle.get();
        } else {
            article = new AuthorArticle();
            article.setUuid(articleData.getUuid()); // Зберігаємо той самий UUID, що згенерував фронтенд
            article.setColumn(column); // ОБОВ'ЯЗКОВО призначаємо колонку
        }

        article.setTitle(articleData.getTitle());
        article.setPostbody(articleData.getPostbody());
        article.setCoverUuid(articleData.getCoverUuid());
        article.setStatus(articleData.getStatus());
        
        AuthorArticle saved = articleService.saveArticle(article);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", 1);
        response.put("uuid", saved.getUuid());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/image/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("image") MultipartFile file) {
        try {
            Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
            String storeUUID = storeService.PutFileToStore(file.getInputStream(), file.getOriginalFilename(), cd, STORE_POSTCOVER);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", 1);
            Map<String, String> fileData = new HashMap<>();
            fileData.put("url", "/store/content/" + storeUUID);
            fileData.put("uuid", storeUUID);
            response.put("file", fileData);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", 0);
            return ResponseEntity.badRequest().body(response);
        }
    }
}
