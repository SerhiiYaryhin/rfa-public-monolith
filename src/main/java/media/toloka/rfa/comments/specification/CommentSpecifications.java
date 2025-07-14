package media.toloka.rfa.comments.specification;

import media.toloka.rfa.comments.model.Comment;
import media.toloka.rfa.comments.model.enumerate.ECommentSourceType;
import media.toloka.rfa.radio.model.Clientdetail;
import org.springframework.data.jpa.domain.Specification;

public class CommentSpecifications {

    public static Specification<Comment> hasSourceType(ECommentSourceType type) {
        return (root, query, cb) -> cb.equal(root.get("commentSourceType"), type);
    }

    public static Specification<Comment> hasTargetUuid(String uuid) {
        return (root, query, cb) -> cb.equal(root.get("targetuuid"), uuid);
    }

    public static Specification<Comment> contentContains(String keyword) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("content")), "%" + keyword.toLowerCase() + "%");
    }

    public static Specification<Comment> hasAuthor(Clientdetail author) {
        return (root, query, cb) -> cb.equal(root.get("autor"), author);
    }
}
