package media.toloka.rfa.radio.admin;

import media.toloka.rfa.radio.admin.service.AdminService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.history.service.HistoryService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Post;
import media.toloka.rfa.radio.model.enumerate.EHistoryType;
import media.toloka.rfa.radio.post.service.PostService;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Date;
import java.util.List;

import static media.toloka.rfa.radio.model.enumerate.EPostStatus.*;

@Profile("Front")
@Controller
public class AdminPosts {

    @Autowired
    private AdminService adminService;

    @Autowired
    private CreaterService createrService;

    @Autowired
    private PostService postService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private HistoryService historyService;


    final Logger logger = LoggerFactory.getLogger(AdminPosts.class);


    @GetMapping(value = "/admin/posts")
    public String getUserHome(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        // Отримуємо пагінований список усіх постів
        org.springframework.data.domain.Page<Post> postPage = postService.GetPostPage(page, 15);

        model.addAttribute("posts", postPage.getContent());
        model.addAttribute("totalPages", postPage.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("linkPage", "/admin/posts");

        return "/admin/posts";
    }

    @GetMapping(value = "/admin/posts/{page}")
    public String getUserHomeOld(@PathVariable Integer page) {
        return "redirect:/admin/posts?page=" + page;
    }

    /**
     * Перемикання статусу публікації поста (схвалено/не схвалено)
     */
    @GetMapping(value = "/admin/togglepost/{postUuid}")
    public String togglePostPublish(@PathVariable String postUuid) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        Post post = postService.GetPostByUuid(postUuid);
        if (post != null) {
            boolean newState = !post.getApruve();
            post.setApruve(newState);
            if (newState) {
                post.setPostStatus(POSTSTATUS_PUBLICATE);
                post.setPublishdate(new Date());
            } else {
                post.setPostStatus(POSTSTATUS_REJECT);
            }
            adminService.SavePost(post);
            
            historyService.saveHistory(EHistoryType.History_PostPublicate, 
                "Admin toggled post status to " + newState + " for post: " + post.getUuid(), 
                post.getClientdetail().getUser());
        }
        return "redirect:/admin/posts";
    }

    @GetMapping(value = "/admin/publishpost/{postUuid}")
    public String getAdminPublishPost(
            @PathVariable String postUuid,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Post post = postService.GetPostByUuid(postUuid);
        post.setPostStatus(POSTSTATUS_PUBLICATE);
        post.setApruve(true);
        post.setPublishdate(new Date());
        adminService.SavePost(post);
        historyService.saveHistory(EHistoryType.History_PostPublicate,"Apruve post "+post.getUuid()
                        +" cd="+post.getClientdetail().getId()
                        +" Client UUID="+post.getClientdetail().getUuid()
                ,post.getClientdetail().getUser());
        return "redirect:/admin/posts";
    }

    @GetMapping(value = "/admin/delpost/{postUuid}")
    public String getAdminDeletePost(
            @PathVariable String postUuid,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Post post = postService.GetPostByUuid(postUuid);
        post.setPostStatus(POSTSTATUS_DELETE);
        post.setApruve(false);
//        post.setPublishdate(new Date());
        adminService.SavePost(post);
        historyService.saveHistory(EHistoryType.History_PostDelete,"Delete post "+post.getUuid()
                        +" cd="+post.getClientdetail().getId()
                        +" Client UUID="+post.getClientdetail().getUuid()
                ,post.getClientdetail().getUser());
        return "redirect:/admin/posts";
    }


    @GetMapping(value = "/admin/rejectpost/{postUuid}")
    public String getAdminRejectPost(
            @PathVariable String postUuid,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Post post = postService.GetPostByUuid(postUuid);
        post.setPostStatus(POSTSTATUS_REJECT);
        post.setApruve(false);
//        post.setPublishdate(new Date());
        adminService.SavePost(post);
        historyService.saveHistory(EHistoryType.History_PostReject,"Reject post id="+post.getUuid()
                +" cd="+post.getClientdetail().getId()
                +" Client UUID="+post.getClientdetail().getUuid()
                ,post.getClientdetail().getUser());
        return "redirect:/admin/posts";
    }
}
