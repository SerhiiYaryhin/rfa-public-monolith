package media.toloka.rfa.comments.repository;

import media.toloka.rfa.comments.model.Comment;
import media.toloka.rfa.comments.model.enumerate.ECommentSourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, String> {

    Page<Comment> findByContentEntityTypeAndContentEntityIdAndParentCommentIsNullOrderByTimestampAsc(
            ECommentSourceType contentEntityType, String contentEntityId, Pageable pageable);

    List<Comment> findByContentEntityTypeAndContentEntityIdAndParentCommentIsNullOrderByTimestampAsc(
            ECommentSourceType contentEntityType, String contentEntityId);

    List<Comment> findByParentCommentAndContentEntityTypeAndContentEntityIdOrderByTimestampAsc(
            Comment parentComment, ECommentSourceType contentEntityType, String contentEntityId);
}