package media.toloka.rfa.convertor;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.convertor.EditorJsConverter;
import media.toloka.rfa.convertor.dto.EditorJsData;
import media.toloka.rfa.radio.model.Post;
import media.toloka.rfa.radio.post.repositore.PostRepositore;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
public class AdminPostController {

    private final PostRepositore postRepository;
    private final EditorJsConverter editorJsConverter;
    private final ObjectMapper objectMapper;

    /**
     * Відкриває сторінку міграції
     */
    @GetMapping("/migration-tool/{uuid}")
    public String showMigrationTool(@PathVariable String uuid, Model model) {
        Post post = postRepository.getByUuid(uuid);
        if (post == null) post = new Post();
        model.addAttribute("post", post);
        return "/convertorToEditors/post-migration";
    }

    /**
     * Повертає дані поста (JSON) для початкового завантаження на сторінці
     */
    @GetMapping("/get-by-uuid/{uuid}")
    @ResponseBody
    public ResponseEntity<?> getPostByUuid(@PathVariable String uuid) {
        Post post = postRepository.getByUuid(uuid);
        if (post == null) return ResponseEntity.notFound().build();

        // Створюємо плоску структуру без циклічних посилань
        return ResponseEntity.ok(Map.of(
                "uuid", post.getUuid(),
                "posttitle", post.getPosttitle() != null ? post.getPosttitle() : "Без заголовка",
                "postbody", post.getPostbody() != null ? post.getPostbody() : ""
        ));
    }

    /**
     * Конвертує HTML поста в блоки Editor.js БЕЗ ЗБЕРЕЖЕННЯ В БД
     */
    @PostMapping("/convert-by-uuid/{uuid}")
    @ResponseBody
    public ResponseEntity<?> convertByUuid(@PathVariable String uuid) {
        Post post = postRepository.getByUuid(uuid);
        if (post == null) return ResponseEntity.notFound().build();

        try {
            String oldHtml = post.getPostbody();

            if (oldHtml == null || oldHtml.trim().isEmpty()) {
                return ResponseEntity.ok(Map.of("data", new EditorJsData()));
            }

            // Виклик вашого Java-конвертера (JSoup)
            EditorJsData data = editorJsConverter.convertHtmlToEditorJs(oldHtml);

            log.info("Preview conversion successful for Post UUID: {}", uuid);

            return ResponseEntity.ok(Map.of(
                    "uuid", post.getUuid(),
                    "data", data
            ));
        } catch (Exception e) {
            log.error("Conversion failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Помилка конвертації: " + e.getMessage());
        }
    }

    /**
     * Метод для фінального збереження, якщо ви вирішите натиснути кнопку "Зберегти"
     */
    @PutMapping("/content-by-uuid/{uuid}")
    @ResponseBody
    public ResponseEntity<?> saveContent(@PathVariable String uuid, @RequestBody String jsonContent) {
        Post post = postRepository.getByUuid(uuid);
        if (post == null) return ResponseEntity.notFound().build();

        post.setPostbody(jsonContent);
        postRepository.save(post);
        log.info("Post UUID: {} content updated by admin", uuid);
        return ResponseEntity.ok(Map.of("status", "success"));
    }
}
