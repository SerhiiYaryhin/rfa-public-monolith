package media.toloka.rfa.comments.controller;

import media.toloka.rfa.comments.model.enumerate.ECommentSourceType;
import media.toloka.rfa.comments.service.CommentService;
import media.toloka.rfa.radio.model.Clientdetail;
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
    public String addReply(@PathVariable ECommentSourceType contentEntityType,
                           @PathVariable String contentEntityId,
                           @RequestParam("parentId") String parentId,
                           @RequestParam("author") String sauthor,
                           @RequestParam("text") String text,
                           RedirectAttributes redirectAttributes) {
        Clientdetail currentUserId = commentService.getCurrentUser();
        Clientdetail author = commentService.getContentAuthorId(contentEntityType, contentEntityId);;
        commentService.saveReply(parentId, currentUserId, text);
        redirectAttributes.addFlashAttribute("message", "Відповідь успішно додана!");
        return "redirect:/universalcomments/" + contentEntityType + "/" + contentEntityId + "/comments";
    }

    @PostMapping("/add")
    public String addRootComment(@PathVariable ECommentSourceType contentEntityType,
                                 @PathVariable String contentEntityId,
                                 @RequestParam("author") String authoruuid,
                                 @RequestParam("text") String text,
                                 RedirectAttributes redirectAttributes) {
        Clientdetail currentUserId = commentService.getCurrentUser();
        Clientdetail author = commentService.getContentAuthorId(contentEntityType, contentEntityId); // todo достати автора посту
        commentService.addRootComment(currentUserId, text, contentEntityType, contentEntityId);
        redirectAttributes.addFlashAttribute("message", "Коментар успішно доданий!");
        return "redirect:/universalcomments/" + contentEntityType + "/" + contentEntityId + "/comments";
    }

    @PostMapping("/update")
    public String updateComment(@PathVariable ECommentSourceType contentEntityType,
                                @PathVariable String contentEntityId,
                                @RequestParam("commentId") String commentId,
                                @RequestParam("newText") String newText,
                                RedirectAttributes redirectAttributes) {
        Clientdetail currentUserId = commentService.getCurrentUser();
        if (commentService.updateComment(commentId, currentUserId, newText)) {
            redirectAttributes.addFlashAttribute("message", "Коментар успішно оновлено!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Не вдалося оновити коментар (можливо, немає прав або не знайдено).");
        }
        return "redirect:/universalcomments/" + contentEntityType + "/" + contentEntityId + "/comments";
    }

    @PostMapping("/delete/{uuid}")
    public String deleteComment(@PathVariable ECommentSourceType contentEntityType,
                                @PathVariable String contentEntityId,
                                @PathVariable String uuid,
                                RedirectAttributes redirectAttributes) {
        Clientdetail currentUserId = commentService.getCurrentUser();
        Clientdetail contentAuthorId = commentService.getContentAuthorId(contentEntityType , contentEntityId);
        if (commentService.deleteComment(uuid, currentUserId, contentAuthorId)) {
            redirectAttributes.addFlashAttribute("message", "Коментар успішно видалено!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Не вдалося видалити коментар (можливо, немає прав).");
        }
        return "redirect:/universalcomments/" + contentEntityType.label + "/" + contentEntityId + "/comments";
    }
}