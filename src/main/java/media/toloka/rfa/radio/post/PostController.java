package media.toloka.rfa.radio.post;

import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Post;
import media.toloka.rfa.radio.model.PostCategory;
import media.toloka.rfa.radio.model.enumerate.EPostCategory;
import media.toloka.rfa.radio.model.enumerate.EPostStatus;
import media.toloka.rfa.radio.post.repositore.PostCategoryRepositore;
import media.toloka.rfa.radio.post.service.PostService;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.fasterxml.jackson.databind.type.LogicalType.Map;
import static media.toloka.rfa.radio.model.enumerate.EPostStatus.POSTSTATUS_REDY;

@Controller
public class PostController {
    final Logger logger = LoggerFactory.getLogger(PostController.class);

//    @Autowired
//    private PostRepositore postRepositore;

//    @Autowired
//    private PostCategoryRepositore postCategoryRepositore;
    @Autowired
    private PostService postService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private CreaterService createrService;

    @Autowired
    private StoreService storeService;

    @GetMapping(value = "/post/postview/{idPost}") // /post/postview/52
    public String getViewPost(
            @PathVariable Long idPost,
            Model model) {
        Post post = postService.GetPostById(idPost);

        if (post == null){
            return "redirect:/";
        }
        post.setLooked( post.getLooked()+1L);
        postService.SavePost(post);

        model.addAttribute("post", post );
        model.addAttribute("ogimage", post.getCoverstoreuuid() );

        return "/post/postview";
    }

    @GetMapping(value = "/creater/editpost/{idPost}")
    public String getCreaterEditPost(
            @PathVariable Long idPost,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (idPost == 0L ) {
            logger.info("Створюємо новий пост");
        }
//        List<Post> posts = createrService.GetAllPostsByCreater(cd);
//        model.addAttribute("posts", posts );
        Post post;

        if (idPost == 0L ) {
            post = new Post();
            post.setId(0L);
        } else {
            post = postService.GetPostById(idPost);
        }

        List<EPostCategory> category = Arrays.asList(EPostCategory.values());
        List<PostCategory> postcategory = new ArrayList<>(); // = postService.getPostCategory();
        for (PostCategory pc : postService.getPostCategory()) {
            if (pc.getParent() == null) {
                postcategory.add(pc);
//                logger.info("видаляємо {}",pc);
//                Boolean pcr = postcategory.remove(pc);
//                logger.info("видалили {}",pcr);
            }
        }

        model.addAttribute("post", post );
        model.addAttribute("categorys", category );
        model.addAttribute("firstpostcategoryslist", postcategory );

        return "/creater/editpost";
    }

    @PostMapping(value="/creater/editpost/{idPost}")
    public String postCreaterEditPost(
            @PathVariable Long idPost,
            @ModelAttribute Post fPost,
            Model model )

    {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        //2024-12-08T19:58:16.827+02:00  WARN 12093 --- [nio-3080-exec-5] .w.s.m.s.DefaultHandlerExceptionResolver :
        // Resolved [org.springframework.web.bind.MethodArgumentNotValidException:
        // Validation failed for argument [1] in public java.lang.String media.toloka.rfa.radio.post.PostController.postCreaterEditPost
        // (java.lang.Long,media.toloka.rfa.radio.model.Post,org.springframework.ui.Model):
        // [Field error in object 'post' on field 'postcategory':
        // rejected value [PostCategory(id=1, uuid=3861283c-3db1-4119-9e5e-67b0267485be, label=Новини, rootPage=true, parent=null)];
        // codes [typeMismatch.post.postcategory,typeMismatch.postcategory,typeMismatch.media.toloka.rfa.radio.model.PostCategory,typeMismatch];
        // arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [post.postcategory,postcategory];
        // arguments []; default message [postcategory]]; default message [Failed to convert property value of type 'java.lang.String'
        // to required type 'media.toloka.rfa.radio.model.PostCategory' for property 'postcategory';
        // Failed to convert from type [java.lang.String] to type [java.lang.Long] f
        // or value [PostCategory(id=1, uuid=3861283c-3db1-4119-9e5e-67b0267485be, label=Новини, rootPage=true, parent=nu (truncated)...]]] ]

        Clientdetail cd = clientService.GetClientDetailByUser(user);
        Post post;
        if (idPost == 0L) {
            logger.info("Створюємо новий пост");
            post = new Post();
            post.setPostStatus(POSTSTATUS_REDY);

        } else {
            post = postService.GetPostById(idPost);
        }
        post.setPostbody(fPost.getPostbody());
        post.setPosttitle(fPost.getPosttitle());
        post.setCategory(fPost.getCategory());
        PostCategory pc = postService.getCategoryByUUID(fPost.getPostcategory().getUuid());
        post.setPostcategory(fPost.getPostcategory());
        post.setClientdetail(cd);

        postService.SavePost(post);

        List<Post> posts = createrService.GetAllPostsByCreater(cd);
        model.addAttribute("posts", posts );
        return "/creater/home";
    }

    @GetMapping(value="/creater/posts")
    public String postCreaterEditPost(
//            @PathVariable Long idPost,
//            @ModelAttribute Post fPost,
            Model model ) {
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());


        List<Post> posts = createrService.GetAllPostsByCreater(cd);
        model.addAttribute("posts", posts );
        return "/creater/posts";
    }

    @GetMapping(value="/creater/publishpost/{idPost}")
    public String postCreaterPublishPost(
            @PathVariable Long idPost,
//            @ModelAttribute Post fPost,
            Model model ) {
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        Post post = postService.GetPostById(idPost);
        if (post != null) {
            post.setPostStatus(EPostStatus.POSTSTATUS_REQUEST);
            postService.SavePost(post);
        }


        List<Post> posts = createrService.GetAllPostsByCreater(cd);
        model.addAttribute("posts", posts );
        return "/creater/posts";
    }

    @GetMapping(value="/creater/delpost/{idPost}")
    public String postCreaterDelPost(
            @PathVariable Long idPost,
//            @ModelAttribute Post fPost,
            Model model ) {
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());

        Post post = postService.GetPostById(idPost);
        if (post != null) {
            post.setPostStatus(EPostStatus.POSTSTATUS_DELETE);
            postService.SavePost(post);
        }

        List<Post> posts = createrService.GetAllPostsByCreater(cd);
        model.addAttribute("posts", posts );
        return "/creater/posts";
    }

// /post/setpostimage/'+${curpost.uuid}+'/'+${storeitem.uuid}
    @GetMapping(value="/post/setpostimage/{uuidpost}/{storeitemuuid}")
    public String SetPostMainImage(
            @PathVariable String uuidpost,
            @PathVariable String storeitemuuid,
            Model model )
    {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Clientdetail cd = clientService.GetClientDetailByUser(user);
        Store store = storeService.GetStoreByUUID(storeitemuuid);
        Post post;
        post = postService.GetByUiid(uuidpost);
        post.setCoverstoreuuid(store.getUuid());
        post.setStore(store);

        postService.SavePost(post);

        List<Post> posts = createrService.GetAllPostsByCreater(cd);
//        model.addAttribute("posts", posts );
        model.addAttribute("post", post );

        return "/creater/editpost";
    }





}
