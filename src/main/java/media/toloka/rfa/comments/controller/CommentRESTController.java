package media.toloka.rfa.comments.controller;

import media.toloka.rfa.comments.model.enumerate.ECommentSourceType;
import media.toloka.rfa.comments.service.CommentService;
import media.toloka.rfa.radio.model.Clientdetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

    @RestController // Змінено на RestController
    @RequestMapping("/api/2.0/universalcomments/{contentEntityType}/{contentEntityId}/comments") // Додано /api/ для розмежування REST API
    public class CommentRESTController {

        private static final Logger log = LoggerFactory.getLogger(CommentRESTController.class); // <-- Додайте цей рядок


        private final CommentService commentService;

        @Autowired
        public CommentRESTController(CommentService commentService) {
            this.commentService = commentService;
        }

        // Примітка: GET-запити для отримання коментарів (наприклад, пагінований список)
        // тепер зазвичай обробляються окремим методом GET.
        // Наприклад: @GetMapping
        // public ResponseEntity<Page<Comment>> getComments(@PathVariable ECommentSourceType contentEntityType,
        //                                                 @PathVariable String contentEntityId,
        //                                                 @RequestParam(defaultValue = "0") int page,
        //                                                 @RequestParam(defaultValue = "10") int size) {
        //     // Логіка для отримання коментарів та повернення їх у вигляді ResponseEntity
        //     return ResponseEntity.ok(commentService.getComments(contentEntityType, contentEntityId, page, size));
        // }


        @PostMapping("/reply")
        public ResponseEntity<Map<String, String>> addReply(@PathVariable ECommentSourceType contentEntityType,
                                                            @PathVariable String contentEntityId,
                                                            @RequestParam("parentId") String parentId,
                                                            @RequestParam("author") String sauthor, // Передається, але, можливо, не використовується
                                                            @RequestParam("text") String text) {
            try {
                // --- Додайте це логування ---
                log.info("Отримано запит на відповідь. parentId: {}, author: {}, text: '{}'", parentId, sauthor, text);
                log.info("Content Entity Type: {}, Content Entity ID: {}", contentEntityType, contentEntityId);
                // ----------------------------
                Clientdetail currentUserId = commentService.getCurrentUser();
                // Clientdetail author = commentService.getContentAuthorId(contentEntityType, contentEntityId); // Цей рядок може бути зайвим тут
                commentService.saveReply(parentId, currentUserId, text);
                Map<String, String> response = new HashMap<>();
                response.put("message", "Відповідь успішно додана!");
                return new ResponseEntity<>(response, HttpStatus.CREATED); // 201 Created
            } catch (Exception e) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("errorMessage", "Помилка при додаванні відповіді: " + e.getMessage());
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST); // 400 Bad Request або 500 Internal Server Error
            }
        }

        @PostMapping("/add")
        public ResponseEntity<Map<String, String>> addRootComment(@PathVariable ECommentSourceType contentEntityType,
                                                                  @PathVariable String contentEntityId,
//                                                                  @RequestParam("author") String authoruuid, // Передається, але, можливо, не використовується
                                                                  @RequestParam("text") String text) {
            try {
                log.info("JVM Default File Encoding: {}", System.getProperty("file.encoding"));
                log.info("JVM Default Charset: {}", java.nio.charset.Charset.defaultCharset().name());

                Clientdetail currentUserId = commentService.getCurrentUser();
                // Clientdetail author = commentService.getContentAuthorId(contentEntityType, contentEntityId); // todo: логіка для отримання автора контенту повинна бути в сервісі або контролері, якщо вона потрібна для валідації/авторизації
                commentService.addRootComment(currentUserId, text, contentEntityType, contentEntityId);
                Map<String, String> response = new HashMap<>();
                response.put("message", "Коментар успішно доданий!");
                return new ResponseEntity<>(response, HttpStatus.CREATED); // 201 Created
            } catch (Exception e) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("errorMessage", "Помилка при додаванні коментаря: " + e.getMessage());
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }
        }

        @PutMapping // Змінено на PUT для оновлення ресурсу
        @RequestMapping("/update") // Залишаємо /update, якщо PUT /commentId неможливий
        public ResponseEntity<Map<String, String>> updateComment(@PathVariable ECommentSourceType contentEntityType,
                                                                 @PathVariable String contentEntityId,
                                                                 @RequestParam("commentId") String commentId,
                                                                 @RequestParam("newText") String newText) {
            try {
                // --- Додайте це логування ---
                log.info("Отримано запит на оновлення коментаря. commentId: {}, newText: '{}'", commentId, newText);
                log.info("Content Entity Type: {}, Content Entity ID: {}", contentEntityId);
                // ----------------------------
                Clientdetail currentUserId = commentService.getCurrentUser();
                if (commentService.updateComment(commentId, currentUserId, newText)) {
                    Map<String, String> response = new HashMap<>();
                    response.put("message", "Коментар успішно оновлено!");
                    return new ResponseEntity<>(response, HttpStatus.OK); // 200 OK
                } else {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("errorMessage", "Не вдалося оновити коментар (можливо, немає прав або не знайдено).");
                    // Можна розрізнити 403 Forbidden (немає прав) і 404 Not Found (не знайдено) в сервісі
                    return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN); // Або HttpStatus.NOT_FOUND
                }
            } catch (Exception e) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("errorMessage", "Помилка при оновленні коментаря: " + e.getMessage());
                return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR); // 500 Internal Server Error
            }
        }

        @DeleteMapping("/delete/{uuid}") // Змінено на DELETE
        public ResponseEntity<Map<String, String>> deleteComment(@PathVariable ECommentSourceType contentEntityType,
                                                                 @PathVariable String contentEntityId,
                                                                 @PathVariable String uuid) {
            try {
                Clientdetail currentUserId = commentService.getCurrentUser();
                Clientdetail contentAuthorId = commentService.getContentAuthorId(contentEntityType , contentEntityId);
                if (commentService.deleteComment(uuid, currentUserId, contentAuthorId)) {
                    Map<String, String> response = new HashMap<>();
                    response.put("message", "Коментар успішно видалено!");
                    return new ResponseEntity<>(response, HttpStatus.OK); // 200 OK або HttpStatus.NO_CONTENT (204) якщо немає тіла відповіді
                } else {
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("errorMessage", "Не вдалося видалити коментар (можливо, немає прав).");
                    return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN); // 403 Forbidden або HttpStatus.NOT_FOUND
                }
            } catch (Exception e) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("errorMessage", "Помилка при видаленні коментаря: " + e.getMessage());
                return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }
