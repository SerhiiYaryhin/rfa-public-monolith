package media.toloka.rfa.comments.repository;

import media.toloka.rfa.comments.model.Comment;
import media.toloka.rfa.comments.model.enumerate.ECommentSourceType;
import media.toloka.rfa.radio.model.Clientdetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, String>, JpaSpecificationExecutor<Comment> {

    // Коментарі до об'єкта певного типу
    List<Comment> findByCommentSourceTypeAndTargetuuid(ECommentSourceType commentSourceType, String targetuuid);

    // Коментарі автора
    List<Comment> findByAutor(Clientdetail autor);

    // За типом об'єкта
    List<Comment> findByCommentSourceType(ECommentSourceType commentSourceType);

    // За targetuuid (усі коментарі до певного об'єкта)
    List<Comment> findByTargetuuid(String targetuuid);

    Comment getByUuid(String uuid);

    Page<Comment> findByCommentSourceTypeAndTargetuuid(ECommentSourceType commentSourceType, String targetuuid, Pageable pageable);

}