package media.toloka.rfa.radio.post.service;

import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Post;
import media.toloka.rfa.radio.model.PostCategory;
import media.toloka.rfa.radio.model.enumerate.EPostCategory;
import media.toloka.rfa.radio.post.repositore.PostCategoryRepositore;
import media.toloka.rfa.radio.post.repositore.PostRepositore;
import media.toloka.rfa.radio.repository.ClientDetailRepository;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {

    final Logger logger = LoggerFactory.getLogger(PostService.class);

    @Autowired
    private PostRepositore postRepositore;

    @Autowired
    private PostCategoryRepositore postCategoryRepositore;

    @Autowired
    private ClientDetailRepository clientDetailRepository;


    public Post GetPostById(Long idPost) {
        Post post = postRepositore.getById(idPost);
        return post;
    }

// Варіант, який працював.
//    public void SavePost(Post post) {
//        postRepositore.save(post);
//    }

    public void SavePost(Post post) {
        Clientdetail cd = post.getClientdetail();

        if (cd != null && cd.getId() != null) {
            cd = clientDetailRepository.findById(cd.getId()).orElseThrow();
            post.setClientdetail(cd);
        }

        postRepositore.save(post);
    }

    public List<Post> GetListPostByApruve(Boolean apruve) {
        return postRepositore.findByApruveOrderByCreatedateDesc(apruve);
    }

    public List<Post> GetAllPosts() {
        return postRepositore.findAllByOrderByCreatedateDesc();
    }

    public Post GetPostByUuid(String uuidpost) {
        return postRepositore.getByUuid(uuidpost);
    }

    public Page<Post> GetPostPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdate").descending());
        return postRepositore.findAll(pageable);
    }

    public List<Post> GetPostsByCategory(EPostCategory postCategory) {
        return postRepositore.getByCategoryOrderByPublishdateDesc(postCategory);
    }
    public List<Post> GetPostsByCategory(PostCategory postCategory) {
        return postRepositore.getByPostcategoryOrderByPublishdateDesc(postCategory);
    }

    public List<PostCategory> getPostCategory() {
        return postCategoryRepositore.findAllByOrderByIdAsc();
    }

    public boolean canUserModifyPost(Post post, Users user) {
        if (post == null || user == null) return false;
        
        // Перевірка, чи є користувач власником посту
        if (post.getClientdetail() != null && 
            post.getClientdetail().getUser() != null && 
            post.getClientdetail().getUser().getId().equals(user.getId())) {
            return true;
        }

        // Перевірка ролей: Редактор, Модератор, Адміністратор
        for (media.toloka.rfa.security.model.Roles role : user.getRoles()) {
            media.toloka.rfa.security.model.ERole eRole = role.getRole();
            if (eRole == media.toloka.rfa.security.model.ERole.ROLE_ADMIN ||
                eRole == media.toloka.rfa.security.model.ERole.ROLE_EDITOR ||
                eRole == media.toloka.rfa.security.model.ERole.ROLE_MODERATOR) {
                return true;
            }
        }

        return false;
    }

    /** Подати пост на модерацію */
    public void requestPublication(Post post) {
        if (post == null) return;
        post.setPostStatus(media.toloka.rfa.radio.model.enumerate.EPostStatus.POSTSTATUS_REQUEST);
        post.setApruve(false);
        postRepositore.save(post);
        logger.info("Пост {} подано на модерацію", post.getUuid());
    }

    /** Відкликати пост з модерації або зняти з публікації */
    public void withdrawToDraft(Post post) {
        if (post == null) return;
        post.setPostStatus(media.toloka.rfa.radio.model.enumerate.EPostStatus.POSTSTATUS_REDY);
        post.setApruve(false);
        postRepositore.save(post);
        logger.info("Пост {} переведено у статус чернетки", post.getUuid());
    }

    /** Видалити пост (soft delete) */
    public void softDeletePost(Post post) {
        if (post == null) return;
        post.setPostStatus(media.toloka.rfa.radio.model.enumerate.EPostStatus.POSTSTATUS_DELETE);
        post.setApruve(false);
        postRepositore.save(post);
        logger.info("Пост {} позначено як видалений", post.getUuid());
    }

    /** Схвалити публікацію (для адміна) */
    public void approvePost(Post post) {
        if (post == null) return;
        post.setApruve(true);
        post.setPostStatus(media.toloka.rfa.radio.model.enumerate.EPostStatus.POSTSTATUS_PUBLICATE);
        post.setApruvedate(new java.util.Date());
        post.setPublishdate(new java.util.Date());
        postRepositore.save(post);
        logger.info("Пост {} схвалено та опубліковано", post.getUuid());
    }

    /** Відхилити публікацію (для адміна) */
    public void rejectPost(Post post) {
        if (post == null) return;
        post.setApruve(false);
        post.setPostStatus(media.toloka.rfa.radio.model.enumerate.EPostStatus.POSTSTATUS_REJECT);
        postRepositore.save(post);
        logger.info("Пост {} відхилено модератором", post.getUuid());
    }

    public List<PostCategory> getChildPostCategory(PostCategory category) {
        return postCategoryRepositore.findByParent(category);
    }

    public PostCategory getCategoryByUUID(String uuid) {
        return postCategoryRepositore.getByUuid(uuid);
    }
}
