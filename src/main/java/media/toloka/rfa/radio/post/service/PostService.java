package media.toloka.rfa.radio.post.service;

import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Post;
import media.toloka.rfa.radio.model.PostCategory;
import media.toloka.rfa.radio.model.enumerate.EPostCategory;
import media.toloka.rfa.radio.post.repositore.PostCategoryRepositore;
import media.toloka.rfa.radio.post.repositore.PostRepositore;
import media.toloka.rfa.radio.repository.ClientDetailRepository;
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

    public List<PostCategory> getChildPostCategory(PostCategory category) {
        return postCategoryRepositore.findByParent(category);
    }

    public PostCategory getCategoryByUUID(String uuid) {
        return postCategoryRepositore.getByUuid(uuid);
    }
}
