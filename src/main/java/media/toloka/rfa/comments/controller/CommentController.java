package media.toloka.rfa.comments.controller;

import media.toloka.rfa.comments.model.Comment;
import media.toloka.rfa.comments.model.enumerate.ECommentSourceType;
import media.toloka.rfa.comments.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/universalcomments/{contentEntityType}/{contentEntityId}/comments")
public class CommentController {

    private final CommentService commentService;

    @Autowired
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    // Примітка: GET-запит на /comments (тобто відображення сторінки з коментарями)
    // тепер обробляється контролером конкретного контенту (наприклад, PostController)
    // Цей контролер обробляє лише POST-запити для дій над коментарями.

    @PostMapping("/reply")
    public String addReply(@PathVariable String contentEntityType,
                           @PathVariable String contentEntityId,
                           @RequestParam("parentId") String parentId,
                           @RequestParam("author") String author,
                           @RequestParam("text") String text,
                           RedirectAttributes redirectAttributes) {
        String currentUserId = commentService.getCurrentUserId();
        commentService.saveReply(parentId, author, currentUserId, text);
        redirectAttributes.addFlashAttribute("message", "Відповідь успішно додана!");
        return "redirect:/" + contentEntityType + "/" + contentEntityId + "/comments";
    }

    @PostMapping("/add")
    public String addRootComment(@PathVariable String contentEntityType,
                                 @PathVariable String contentEntityId,
                                 @RequestParam("author") String author,
                                 @RequestParam("text") String text,
                                 RedirectAttributes redirectAttributes) {
        String currentUserId = commentService.getCurrentUserId();
        commentService.addRootComment(author, currentUserId, text, contentEntityType, contentEntityId);
        redirectAttributes.addFlashAttribute("message", "Коментар успішно доданий!");
        return "redirect:/" + contentEntityType + "/" + contentEntityId + "/comments";
    }

    @PostMapping("/update")
    public String updateComment(@PathVariable String contentEntityType,
                                @PathVariable String contentEntityId,
                                @RequestParam("commentId") String commentId,
                                @RequestParam("newText") String newText,
                                RedirectAttributes redirectAttributes) {
        String currentUserId = commentService.getCurrentUserId();
        if (commentService.updateComment(commentId, currentUserId, newText)) {
            redirectAttributes.addFlashAttribute("message", "Коментар успішно оновлено!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Не вдалося оновити коментар (можливо, немає прав або не знайдено).");
        }
        return "redirect:/" + contentEntityType + "/" + contentEntityId + "/comments";
    }

    @PostMapping("/delete/{id}")
    public String deleteComment(@PathVariable String contentEntityType,
                                @PathVariable String contentEntityId,
                                @PathVariable String id,
                                RedirectAttributes redirectAttributes) {
        String currentUserId = commentService.getCurrentUserId();
        String contentAuthorId = commentService.getContentAuthorId(contentEntityType, contentEntityId);
        if (commentService.deleteComment(id, currentUserId, contentAuthorId)) {
            redirectAttributes.addFlashAttribute("message", "Коментар успішно видалено!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Не вдалося видалити коментар (можливо, немає прав).");
        }
        return "redirect:/" + contentEntityType + "/" + contentEntityId + "/comments";
    }
}