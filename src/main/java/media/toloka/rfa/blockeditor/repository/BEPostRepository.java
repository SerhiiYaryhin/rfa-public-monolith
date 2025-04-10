package media.toloka.rfa.blockeditor.repository;

import media.toloka.rfa.blockeditor.model.BlockPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BEPostRepository extends JpaRepository<BlockPost, Long> {}