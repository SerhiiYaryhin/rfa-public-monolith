package media.toloka.rfa.comments.controller;

import media.toloka.rfa.comments.model.Comment;
import media.toloka.rfa.comments.model.enumerate.ECommentSourceType;
import media.toloka.rfa.comments.service.CommentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/{type}/{targetUuid}")
    public String getComments(
//            @PathVariable String stype,
            @PathVariable ECommentSourceType type,
            @PathVariable String targetUuid,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
//        ECommentSourceType type;
//        type =
        Page<Comment> comments = commentService.getComments(type, targetUuid, PageRequest.of(page, 5));
        model.addAttribute("comments", comments);
        model.addAttribute("type", type.label);
        model.addAttribute("targetUuid", targetUuid);
        return "/comment/comments";
    }

    @PostMapping("/add")
    public String addComment(
            @RequestParam String commentSourceType,
            @RequestParam String targetuuid,
            @RequestParam String content
    ) {
        commentService.addComment(commentSourceType, targetuuid, content);
        return "redirect:/comments/" + commentSourceType + "/" + targetuuid;
    }

    @PostMapping("/edit")
    public String editComment(
            @RequestParam String uuid,
            @RequestParam String content
    ) {
        commentService.editComment(uuid, content);
        return "redirect:/comments/" + "";
//        return "redirect:/comments";
    }
}


//
//import lombok.RequiredArgsConstructor;
//import media.toloka.rfa.comments.model.Comment;
//import media.toloka.rfa.comments.model.enumerate.ECommentSourceType;
//import media.toloka.rfa.comments.service.CommentService;
//import media.toloka.rfa.radio.model.Clientdetail;
//import org.springframework.data.domain.Page;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.*;
//
//@Controller
//@RequiredArgsConstructor
//@RequestMapping("/comments")
//public class CommentController {
//
//    private final CommentService commentService;
//
//    @GetMapping("/{type}/{targetUuid}")
//    public String viewComments(@PathVariable ECommentSourceType type,
//                               @PathVariable String targetUuid,
//                               @RequestParam(defaultValue = "0") int page,
//                               @RequestParam(defaultValue = "5") int size,
//                               Model model) {
//        Page<Comment> comments = commentService.getComments(type, targetUuid, page, size);
//        model.addAttribute("comments", comments);
//        model.addAttribute("type", type);
//        model.addAttribute("targetUuid", targetUuid);
//        return "comments";
//    }
//
//    @PostMapping("/add")
//    public String addComment(@ModelAttribute Comment comment) {
//        commentService.saveComment(comment);
//        return "redirect:/comments/" + comment.getCommentSourceType() + "/" + comment.getTargetuuid();
//    }
//
//    @PostMapping("/edit")
//    public String editComment(@RequestParam String uuid,
//                              @RequestParam String content,
//                              @SessionAttribute("user") Clientdetail user) {
//        commentService.updateCommentContent(uuid, content, user);
//        return "redirect:/comments";
//    }
//}