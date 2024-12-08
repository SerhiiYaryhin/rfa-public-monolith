package media.toloka.rfa.radio.post.repositore;

import media.toloka.rfa.radio.model.Post;
import media.toloka.rfa.radio.model.PostCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface PostCategoryRepositore  extends JpaRepository<PostCategory, Long> {

    PostCategory getById(Long aLong);
    PostCategory getByUuid(String uuid);
//    List<PostCategory> findByParent(String uuid);
    List<PostCategory> findByParent(PostCategory pc);
}
