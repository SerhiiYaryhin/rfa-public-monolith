package media.toloka.rfa.radio.post.service;

import media.toloka.rfa.radio.model.Post;
import media.toloka.rfa.radio.model.enumerate.EPostCategory;
import media.toloka.rfa.radio.post.repositore.PostRepositore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {

    final Logger logger = LoggerFactory.getLogger(PostService.class);

    @Autowired
    private PostRepositore postRepositore;


    public Post GetPostById(Long idPost) {
        Post post = postRepositore.getById(idPost);
        return post;
    }

    public void SavePost(Post post) {
        postRepositore.save(post);
    }

    public List<Post> GetListPostByApruve(Boolean apruve) {
        return postRepositore.findByApruveOrderByCreatedateDesc(apruve);
    }

    public Post GetByUiid(String uuidpost) {
        return postRepositore.getByUuid(uuidpost);
    }

    public List<Post> GetPostsByCategory(EPostCategory postCategory) {
        return postRepositore.getByCategory(postCategory);
    }
}
