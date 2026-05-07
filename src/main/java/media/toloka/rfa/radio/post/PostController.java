package media.toloka.rfa.radio.post;

import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.fasterxml.jackson.databind.type.LogicalType.Map;
import static media.toloka.rfa.radio.model.enumerate.EPostStatus.*;

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


    @GetMapping(value = "/post/postview/{uuidPost}")
    public String getViewPost(
            @PathVariable String uuidPost,
            @RequestParam (defaultValue = "0")  Integer page,
            @RequestParam (defaultValue = "5")  Integer size,
            Model model) {
        Post post = postService.GetPostByUuid(uuidPost);

        if (post == null) {
            return "redirect:/";
        }
        post.setLooked(post.getLooked() + 1L);
        postService.SavePost(post);

        model.addAttribute("post", post);
        model.addAttribute("ogimage", post.getCoverstoreuuid());
        model.addAttribute("stationsonline", StationOnlineList.getInstance().GetOnlineList());
        model.addAttribute("storeService", commentService.getStoreService());

        // --- Завантаження коментарів ---
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> commentsPage = commentService.getPaginatedCommentsHierarchy(ECommentSourceType.COMMENT_POST, post.getUuid(), pageable);

        model.addAttribute("commentsPage", commentsPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", commentsPage.getTotalPages());
        if (commentService.getCurrentUserId() != null) {
            model.addAttribute("currentUserId", commentService.getCurrentUserId().getUuid());
        } else model.addAttribute("currentUserId", null);
        model.addAttribute("contentAuthorId", post.getClientdetail().getUuid() );
        model.addAttribute("contentEntityType", ECommentSourceType.COMMENT_POST);
        model.addAttribute("contentEntityId", post.getUuid());

        return "/post/postview";
    }

    @GetMapping(value = "/creater/editpost/{uuidPost}")
    public String getCreaterEditPost(
            @PathVariable String uuidPost,
            Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Post post;
        if (uuidPost.equals("0")) {
            logger.info("Створюємо новий пост");
            post = new Post();
            // ID залишається null для нового посту, UUID генерується в моделі
        } else {
            post = postService.GetPostByUuid(uuidPost);
        }

        if (post == null) return "redirect:/creater/home";

        List<EPostCategory> category = Arrays.asList(EPostCategory.values());
        List<PostCategory> postcategory = new ArrayList<>();
        for (PostCategory pc : postService.getPostCategory()) {
            if (pc.getParent() == null) {
                postcategory.add(pc);
            }
        }

        model.addAttribute("post", post);
        model.addAttribute("categorys", category);
        model.addAttribute("postStatuses", Arrays.asList(EPostStatus.POSTSTATUS_REDY, EPostStatus.POSTSTATUS_REQUEST));
        model.addAttribute("firstpostcategoryslist", postcategory);

        return "/creater/editpost";
    }

    @PostMapping(value = "/creater/editpost/{uuidPost}")
    public String postCreaterEditPost(
            @PathVariable String uuidPost,
            @ModelAttribute Post fPost,
            HttpServletRequest request,
            Model model) {

        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(user);
        
        Post post = postService.GetPostByUuid(uuidPost);
        
        // Якщо пост не знайдено в базі, це новий пост
        if (post == null) {
            // Перевіряємо, чи UUID в моделі співпадає з тим, що в URL
            if (uuidPost.equals(fPost.getUuid())) {
                // Це новий пост, створюємо новий об'єкт
                post = fPost;
                post.setId(null); // Переконуємося, що ID порожній для нового запису
                post.setPostStatus(POSTSTATUS_REDY);
                post.setClientdetail(cd);
            } else {
                // Невідповідність UUID - помилка
                return "redirect:/creater/home";
            }
        } else {
            // Це існуючий пост, оновлюємо його
            // Валідація ілюстрації при запиті на публікацію
            if (fPost.getPostStatus() == POSTSTATUS_REQUEST && (post.getCoverstoreuuid() == null || post.getCoverstoreuuid().isEmpty())) {
                model.addAttribute("error", "Для публікації посту необхідно встановити головну ілюстрацію!");
                model.addAttribute("post", post);
                model.addAttribute("categorys", Arrays.asList(EPostCategory.values()));
                model.addAttribute("postStatuses", Arrays.asList(EPostStatus.POSTSTATUS_REDY, EPostStatus.POSTSTATUS_REQUEST));
                List<PostCategory> pcats = new ArrayList<>();
                for (PostCategory pc : postService.getPostCategory()) {
                    if (pc.getParent() == null) pcats.add(pc);
                }
                model.addAttribute("firstpostcategoryslist", pcats);
                return "/creater/editpost";
            }

            post.setPostbody(fPost.getPostbody());
            post.setPosttitle(fPost.getPosttitle());
            post.setLead(fPost.getLead());
            post.setCategory(fPost.getCategory());
            post.setPostcategory(fPost.getPostcategory());
            post.setPostStatus(fPost.getPostStatus());
        }

        postService.SavePost(post);

        return "redirect:/creater/home";
    }

    @GetMapping(value = "/creater/posts")
    public String getCreaterPosts(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";
        
        Clientdetail cd = clientService.GetClientDetailByUser(user);

        Integer curpage = page;
        Page<Post> pageStore = createrService.GetPostPageByClientDetail(curpage, 10, cd);
        List<Post> viewList = pageStore.getContent();

        model.addAttribute("viewList", viewList);
        model.addAttribute("totalPages", pageStore.getTotalPages());
        model.addAttribute("currentPage", curpage);
        model.addAttribute("linkPage", "/creater/posts");

        return "/creater/posts";
    }

    @GetMapping(value = "/creater/posts/{cPage}")
    public String getCreaterPostsOld(@PathVariable String cPage) {
        return "redirect:/creater/posts?page=" + cPage;
    }

    @GetMapping(value = "/creater/publishpost/{uuidPost}")
    public String postCreaterPublishPost(
            @PathVariable String uuidPost,
            Model model) {
        Post post = postService.GetPostByUuid(uuidPost);
        if (post != null) {
            post.setPostStatus(EPostStatus.POSTSTATUS_REQUEST);
            postService.SavePost(post);
        }
        return "redirect:/creater/home";
    }

    @GetMapping(value = "/creater/delpost/{uuidPost}")
    public String postCreaterDelPost(
            @PathVariable String uuidPost,
            Model model) {
        Post post = postService.GetPostByUuid(uuidPost);
        if (post != null) {
            post.setPostStatus(EPostStatus.POSTSTATUS_DELETE);
            postService.SavePost(post);
        }
        return "redirect:/creater/home";
    }

    @GetMapping(value = "/post/setpostimage/{uuidpost}/{storeitemuuid}")
    public String SetPostMainImage(
            @PathVariable String uuidpost,
            @PathVariable String storeitemuuid,
            Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Post post = postService.GetPostByUuid(uuidpost);
        if (post == null) {
            logger.error("Пост з UUID {} не знайдено", uuidpost);
            return "redirect:/creater/home";
        }

        Store store = storeService.GetStoreByUUID(storeitemuuid);
        if (store != null) {
            post.setCoverstoreuuid(store.getUuid());
            postService.SavePost(post);
            logger.info("Призначено ілюстрацію {} для посту {}", storeitemuuid, uuidpost);
        }

        return "redirect:/creater/editpost/" + post.getUuid();
    }


}
