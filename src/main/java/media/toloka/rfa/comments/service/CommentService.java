package media.toloka.rfa.comments.service;

import media.toloka.rfa.comments.model.Comment;
import media.toloka.rfa.comments.model.enumerate.ECommentSourceType;
import media.toloka.rfa.comments.repository.CommentRepository;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.post.service.PostService;
import media.toloka.rfa.security.model.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CommentService {

    @Autowired
    private ClientService clientService;

    @Autowired
    private PostService postService;

    @Autowired
    private CreaterService createrService;


    private final CommentRepository commentRepository;

    private static final String POST_AUTHOR_ID = "admin123"; // Для демонстрації

    @Autowired
    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
//        if (commentRepository.count() == 0) {
//            initSampleData();
//        }
    }

    @Transactional
    public void saveComment(Comment comment) {
        commentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public List<Comment> getListCommentsHierarchy(ECommentSourceType contentEntityType, String contentEntityId) {
        List<Comment> rootCommentsList = commentRepository.findByContentEntityTypeAndContentEntityIdAndParentCommentIsNullOrderByTimestampAsc(
                contentEntityType, contentEntityId);

        for (Comment root : rootCommentsList) {
            loadRepliesRecursively(root, 1, contentEntityType, contentEntityId);
        }
        return rootCommentsList;
    }


    @Transactional(readOnly = true)
    public Page<Comment> getPaginatedCommentsHierarchy(ECommentSourceType contentEntityType, String contentEntityId, Pageable pageable) {
        Page<Comment> rootCommentsPage = commentRepository.findByContentEntityTypeAndContentEntityIdAndParentCommentIsNullOrderByTimestampAsc(
                contentEntityType, contentEntityId, pageable);

        for (Comment root : rootCommentsPage.getContent()) {
            loadRepliesRecursively(root, 1, contentEntityType, contentEntityId);
        }
        return rootCommentsPage;
    }

    private void loadRepliesRecursively(Comment parent, int currentDepth, ECommentSourceType contentEntityType, String contentEntityId) {
        if (currentDepth > 5) {
            return;
        }

        List<Comment> directReplies = commentRepository.findByParentCommentAndContentEntityTypeAndContentEntityIdOrderByTimestampAsc(
                parent, contentEntityType, contentEntityId);
        parent.setReplies(directReplies);

        for (Comment reply : directReplies) {
            loadRepliesRecursively(reply, currentDepth + 1, contentEntityType, contentEntityId);
        }
    }

    @Transactional
    public void addRootComment(Clientdetail author, String text, ECommentSourceType contentEntityType, String contentEntityId) {
        Comment newComment = new Comment(author, text, contentEntityType, contentEntityId);
        saveComment(newComment);
    }

    @Transactional
    public void saveReply(String parentId, Clientdetail author, String text) {
        Optional<Comment> parentCommentOpt = commentRepository.findById(parentId);
        if (parentCommentOpt.isPresent()) {
            Comment parentComment = parentCommentOpt.get();
            int newDepth = parentComment.getDepth() + 1;

            if (newDepth <= 5) {
                Comment reply = new Comment(author, text, parentComment, newDepth,
                        parentComment.getContentEntityType(), parentComment.getContentEntityId());
                parentComment.addReply(reply);
                commentRepository.save(parentComment);
            } else {
                System.out.println("Досягнуто максимальну глибину вкладення коментарів (5). Відповідь не збережено.");
            }
        } else {
            System.out.println("Батьківський коментар з ID " + parentId + " не знайдено.");
        }
    }

    @Transactional
    public boolean updateComment(String commentId, Clientdetail currentUserId, String newText) {
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isPresent()) {
            Comment comment = commentOpt.get();
            if (comment.getAuthor().getUuid().equals(currentUserId.getUuid())) {
                comment.setText(newText);
                commentRepository.save(comment);
                return true;
            } else {
                System.out.println("Користувач " + currentUserId + " не має прав для редагування коментаря " + commentId);
                return false;
            }
        }
        return false;
    }

    @Transactional
    public boolean deleteComment(String commentId, Clientdetail currentUser, Clientdetail contentAuthor) {
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isPresent()) {
            Comment comment = commentOpt.get();
            if (comment.getAuthor().getUuid().equals(currentUser.getUuid())
                    || contentAuthor.getUuid().equals(currentUser.getUuid())) {
                Comment parentComment = comment.getParentComment();
                if (parentComment != null) {
                    comment.getParentComment().removeReply(comment);
                    commentRepository.save(parentComment);
                }
                commentRepository.delete(comment);
                return true;
            } else {
                System.out.println("Користувач " + currentUser.getUuid() + " не має прав для видалення коментаря " + commentId);
                return false;
            }
        }
        return false;
    }

    public Clientdetail getCurrentUser() {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return null;
        }

        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        return cd; // Заглушка: ID поточного користувача
    }

    public Clientdetail getContentAuthorId(ECommentSourceType contentEntityType, String contentEntityId) {
        // У реальному додатку: зверніться до сервісу конкретного контенту
        // Наприклад: postService.getAuthorId(contentEntityId);
        // Для демонстрації, повертаємо фіксований ID
        Clientdetail cd;
        switch (contentEntityType) {
            case ECommentSourceType.COMMENT_POST:
                cd = postService.GetByUiid(contentEntityId).getClientdetail();
                break;
            case ECommentSourceType.COMMENT_TRACK:
                cd = createrService.GetTrackByUuid(contentEntityId).getClientdetail();
                break;

            default:
                cd = null;
        }


        return cd;
    }

    public Clientdetail getCurrentUserId() {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return null;
        }

        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        return cd;
    }

}