package media.toloka.rfa.blockeditor.repository;

import media.toloka.editor.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {}