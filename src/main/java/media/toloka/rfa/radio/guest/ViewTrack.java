package media.toloka.rfa.radio.guest;

import media.toloka.rfa.comments.model.Comment;
import media.toloka.rfa.comments.model.enumerate.ECommentSourceType;
import media.toloka.rfa.comments.service.CommentService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.model.Track;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Profile("Front")
@Controller
public class ViewTrack {

    @Autowired
    private CreaterService createrService;

    @Autowired
    private CommentService commentService;

    // сайт світимо опис треку
    @GetMapping(value = "/guest/viewtrack/{trackuuid}")
    public String getTracksAll(
            @PathVariable String trackuuid,
            @RequestParam(defaultValue = "0")  Integer page,
            @RequestParam (defaultValue = "5")  Integer size,
            Model model) {
        Track curtrack = createrService.GetTrackByUuid(trackuuid);
        if (curtrack != null)   {
            // model.addAttribute("curtrack", curtrack );
            model.addAttribute("curtrack", curtrack );

            // --- Завантаження коментарів ---
            Pageable pageable = PageRequest.of(page, size);
            Page<Comment> commentsPage = commentService.getPaginatedCommentsHierarchy(ECommentSourceType.COMMENT_TRACK, curtrack.getUuid(), pageable);

            // --- Передача даних у Model для Thymeleaf ---
            model.addAttribute("commentsPage", commentsPage);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", commentsPage.getTotalPages());
            if (commentService.getCurrentUserId() != null) {
                model.addAttribute("currentUserId", commentService.getCurrentUserId().getUuid());
            } else model.addAttribute("currentUserId", null);
//        model.addAttribute("currentUserId", commentService.getCurrentUserId().getUuid());
            model.addAttribute("contentAuthorId", curtrack.getClientdetail().getUuid() );
//        model.addAttribute("contentAuthorId", commentService.getContentAuthorId(ECommentSourceType.COMMENT_POST, post.getUuid()));
            model.addAttribute("contentEntityType", ECommentSourceType.COMMENT_TRACK);
            model.addAttribute("contentEntityId", curtrack.getUuid());
            return "/guest/viewtrack";
        }
        return "redirect:/";
    }
}
