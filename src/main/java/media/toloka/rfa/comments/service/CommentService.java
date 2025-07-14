package media.toloka.rfa.comments.service;


import media.toloka.rfa.comments.model.Comment;
import media.toloka.rfa.comments.model.enumerate.ECommentSourceType;
import media.toloka.rfa.comments.repository.CommentRepository;
import media.toloka.rfa.radio.model.Clientdetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class CommentService {

    private final CommentRepository commentRepository;

    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public Page<Comment> getComments(ECommentSourceType type, String targetUuid, Pageable pageable) {
        return commentRepository.findByCommentSourceTypeAndTargetuuid(type, targetUuid, pageable);
    }

    public void addComment(String sourceTypeLabel, String targetuuid, String content) {
        Comment comment = new Comment();
        comment.setCommentSourceType(ECommentSourceType.fromLabel(sourceTypeLabel));
        comment.setTargetuuid(targetuuid);
        comment.setContent(content);
        comment.setCreatedate(new Date());
        commentRepository.save(comment);
    }

    public void editComment(String uuid, String content) {
        Comment comment = commentRepository.findById(uuid).orElseThrow();
        comment.setContent(content);
        commentRepository.save(comment);
    }

    // Генеруємо колір фону з рядка (наприклад, імені)
    public String generateColorFromString(String input) {
        if (input == null || input.isEmpty()) {
            return "#6c757d"; // дефолтний сірий
        }
        int hash = input.hashCode();
        int r = (hash & 0xFF0000) >> 16;
        int g = (hash & 0x00FF00) >> 8;
        int b = (hash & 0x0000FF);

        // Зміщуємо кольори до світліших тонів
        r = (r + 128) / 2;
        g = (g + 128) / 2;
        b = (b + 128) / 2;

        return String.format("#%02x%02x%02x", r, g, b);
    }

    // Обчислюємо контрастний колір тексту (білий або чорний)
    public String getContrastColor(String hexColor) {
        if (hexColor == null || !hexColor.startsWith("#") || hexColor.length() != 7) {
            return "#000000"; // чорний за замовчуванням
        }

        int r = Integer.parseInt(hexColor.substring(1, 3), 16);
        int g = Integer.parseInt(hexColor.substring(3, 5), 16);
        int b = Integer.parseInt(hexColor.substring(5, 7), 16);

        double luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;

        return luminance > 0.5 ? "#000000" : "#FFFFFF";
    }

    // Отримуємо ініціали користувача
    public String getInitials(Clientdetail user) {
        if (user == null) return "?";

        String firstName = user.getCustname() != null ? user.getCustname().trim() : "A";
        String lastName = user.getCustsurname() != null ? user.getCustsurname().trim() : "S";

        String initials = "";
        if (!firstName.isEmpty()) initials += firstName.charAt(0);
        if (!lastName.isEmpty()) initials += lastName.charAt(0);

        return initials.toUpperCase();
    }
}


//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import media.toloka.rfa.comments.model.Comment;
//import media.toloka.rfa.comments.model.enumerate.ECommentSourceType;
//import media.toloka.rfa.comments.repositore.CommentRepository;
//import media.toloka.rfa.radio.model.Clientdetail;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.stereotype.Service;
//
//import java.util.Optional;
//
//@Service
//@RequiredArgsConstructor
//public class CommentService {
//
//    private final CommentRepository commentRepository;
//
//    public Page<Comment> getComments(ECommentSourceType sourceType, String targetUuid, int page, int size) {
//        Pageable pageable = PageRequest.of(page, size, Sort.by("createdate").descending());
//        return commentRepository.findByCommentSourceTypeAndTargetuuid(sourceType, targetUuid, pageable);
//    }
//
//    @Transactional
//    public Comment saveComment(Comment comment) {
//        return commentRepository.save(comment);
//    }
//
//    public Optional<Comment> getByUuid(String uuid) {
//        return commentRepository.findById(uuid);
//    }
//
//    public boolean isAuthor(String commentUuid, Clientdetail user) {
//        return commentRepository.findById(commentUuid)
//                .map(comment -> comment.getAutor().equals(user))
//                .orElse(false);
//    }
//
//    @Transactional
//    public void updateCommentContent(String uuid, String newContent, Clientdetail user) {
//        Comment comment = commentRepository.findById(uuid)
//                .orElseThrow(() -> new RuntimeException("Comment not found"));
//        if (!comment.getAutor().equals(user)) {
//            throw new SecurityException("You are not the author");
//        }
//        comment.setContent(newContent);
//        commentRepository.save(comment);
//    }
//}
