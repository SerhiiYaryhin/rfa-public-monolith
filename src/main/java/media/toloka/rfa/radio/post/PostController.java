package media.toloka.rfa.radio.post;

import media.toloka.rfa.comments.model.Comment;
import media.toloka.rfa.comments.model.enumerate.ECommentSourceType;
import media.toloka.rfa.comments.service.CommentService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Post;
import media.toloka.rfa.radio.model.PostCategory;
import media.toloka.rfa.radio.model.enumerate.EPostCategory;
import media.toloka.rfa.radio.model.enumerate.EPostStatus;
import media.toloka.rfa.radio.post.repositore.PostCategoryRepositore;
import media.toloka.rfa.radio.post.service.PostService;
import media.toloka.rfa.radio.station.onlinelist.Model.ListOnlineFront;
import media.toloka.rfa.radio.station.service.StationOnlineList;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.fasterxml.jackson.databind.type.LogicalType.Map;
import static media.toloka.rfa.radio.model.enumerate.EPostStatus.POSTSTATUS_REDY;

@Profile("Front")
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

    @Autowired
    private CommentService commentService;


    @GetMapping(value = "/post/postview/{idPost}") // /post/postview/52
    public String getViewPost(
            @PathVariable Long idPost,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        Post post = postService.GetPostById(idPost);

        if (post == null) {
            return "redirect:/";
        }
        post.setLooked(post.getLooked() + 1L);
        postService.SavePost(post);

//        List<ListOnlineFront> stationOnlineList = StationOnlineList.getInstance().GetOnlineList();

        model.addAttribute("post", post);
        model.addAttribute("ogimage", post.getCoverstoreuuid());
        model.addAttribute("stationsonline", StationOnlineList.getInstance().GetOnlineList());

        String type = "POST";
        Page<Comment> comments = commentService.getComments(ECommentSourceType.fromLabel(type), post.getUuid(), PageRequest.of(page, 5));
        model.addAttribute("comments", comments);
        model.addAttribute("type", type);
        model.addAttribute("targetUuid", post.getUuid());

        return "/post/postview";
    }

    @GetMapping(value = "/creater/editpost/{idPost}")
    public String getCreaterEditPost(
            @PathVariable Long idPost,
            Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (idPost == 0L) {
            logger.info("Створюємо новий пост");
        }
//        List<Post> posts = createrService.GetAllPostsByCreater(cd);
//        model.addAttribute("posts", posts );
        Post post;

        if (idPost == 0L) {
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
            }
        }

        model.addAttribute("post", post);
        model.addAttribute("categorys", category);
        model.addAttribute("firstpostcategoryslist", postcategory);

        return "/creater/editpost";
    }

    @PostMapping("/creater/editpostcomment")
    public String editPostComment(
            @RequestParam String uuid,
            @RequestParam String content,
            @RequestParam Integer postid,
            @RequestParam Integer page

    ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(user);
        if (cd.getUuid().contains(commentService.GetByUUID(uuid).getUuid())) commentService.editComment(uuid, content);
        return "redirect:/post/postview/" + postid.toString() + "?page=" + page.toString();
//        return "redirect:/comments";
    }

    @PostMapping(value = "/creater/editpost/{idPost}")
    public String postCreaterEditPost(
            @PathVariable Long idPost,
            @ModelAttribute Post fPost,
            Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
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
//        PostCategory pc = postService.getCategoryByUUID(fPost.getPostcategory().getUuid());
        post.setPostcategory(fPost.getPostcategory());
        post.setClientdetail(cd);


        postService.SavePost(post);


        Integer curpage = 0;
        Page pageStore = createrService.GetPostPageByClientDetail(curpage, 10, cd);
        List<Post> viewList = pageStore.stream().toList();

        model.addAttribute("viewList", viewList);
        model.addAttribute("totalPages", pageStore.getTotalPages());
        model.addAttribute("currentPage", curpage);
        model.addAttribute("linkPage", "/creater/posts/");
        return "/creater/home";
    }

    @GetMapping(value = "/creater/posts/{cPage}")
    public String postCreaterEditPost(
            @PathVariable String cPage,
//            @ModelAttribute Post fPost,
            Model model) {
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());

//        List<Post> posts = createrService.GetAllPostsByCreater(cd);
//        model.addAttribute("posts", posts );

        Integer curpage = Integer.parseInt(cPage);
        Page pageStore = createrService.GetPostPageByClientDetail(curpage, 10, cd);
        List<Post> viewList = pageStore.stream().toList();

        model.addAttribute("viewList", viewList);
        model.addAttribute("totalPages", pageStore.getTotalPages());
        model.addAttribute("currentPage", curpage);
        model.addAttribute("linkPage", "/creater/posts/");

        return "/creater/posts";
    }

    @GetMapping(value = "/creater/publishpost/{idPost}")
    public String postCreaterPublishPost(
            @PathVariable Long idPost,
//            @ModelAttribute Post fPost,
            Model model) {
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        Post post = postService.GetPostById(idPost);
        if (post != null) {
            post.setPostStatus(EPostStatus.POSTSTATUS_REQUEST);
            postService.SavePost(post);
        }


//        List<Post> posts = createrService.GetAllPostsByCreater(cd);
//        model.addAttribute("posts", posts );

        Integer curpage = 0;
        Page pageStore = createrService.GetPostPageByClientDetail(curpage, 10, cd);
        List<Post> viewList = pageStore.stream().toList();

        model.addAttribute("viewList", viewList);
        model.addAttribute("totalPages", pageStore.getTotalPages());
        model.addAttribute("currentPage", curpage);
        model.addAttribute("linkPage", "/creater/posts/");


        return "/creater/posts";
    }

    @GetMapping(value = "/creater/delpost/{idPost}")
    public String postCreaterDelPost(
            @PathVariable Long idPost,
//            @ModelAttribute Post fPost,
            Model model) {
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());

        Post post = postService.GetPostById(idPost);
        if (post != null) {
            post.setPostStatus(EPostStatus.POSTSTATUS_DELETE);
            postService.SavePost(post);
        }

//        List<Post> posts = createrService.GetAllPostsByCreater(cd);
//        model.addAttribute("posts", posts );

        Integer curpage = 0;
        Page<Post> pageStore = createrService.GetPostPageByClientDetail(curpage, 10, cd);
        List<Post> viewList = pageStore.stream().toList();

        model.addAttribute("viewList", viewList);
        model.addAttribute("totalPages", pageStore.getTotalPages());
        model.addAttribute("currentPage", curpage);
        model.addAttribute("linkPage", "/creater/posts/");

        return "/creater/posts";
    }

    // /post/setpostimage/'+${curpost.uuid}+'/'+${storeitem.uuid}
    @GetMapping(value = "/post/setpostimage/{uuidpost}/{storeitemuuid}")
    public String SetPostMainImage(
            @PathVariable String uuidpost,
            @PathVariable String storeitemuuid,
            Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Clientdetail cd = clientService.GetClientDetailByUser(user);
        Store store = storeService.GetStoreByUUID(storeitemuuid);
        Post post = postService.GetByUiid(uuidpost);
        post.setCoverstoreuuid(store.getUuid());
//        post.setStore(store);

        postService.SavePost(post);

        List<Post> posts = createrService.GetAllPostsByCreater(cd);
//        model.addAttribute("posts", posts );
        model.addAttribute("post", post);

        return "/creater/editpost";
    }


}
