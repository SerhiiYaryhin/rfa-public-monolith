package media.toloka.rfa.radio.post.repositore;

import media.toloka.rfa.radio.model.PostCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostCategoryRepositore  extends JpaRepository<PostCategory, Long> {

    PostCategory getById(Long aLong);
    PostCategory getByUuid(String uuid);
//    List<PostCategory> findByUuid(String uuid);
    List<PostCategory> findByParent(PostCategory pc);

    List<PostCategory> findAllByOrderByIdAsc();

    List<PostCategory> findAllByOrderByIdDesc();
}
