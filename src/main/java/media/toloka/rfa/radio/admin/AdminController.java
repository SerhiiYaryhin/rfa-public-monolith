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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Date;
import java.util.List;

import static media.toloka.rfa.radio.model.enumerate.EPostStatus.*;

@Controller
public class AdminController {

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


    final Logger logger = LoggerFactory.getLogger(AdminController.class);


    @GetMapping(value = "/admin/home")
    public String getUserHome(
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        List<Post> posts = adminService.GetNotApruvePosts();
        model.addAttribute("posts", posts );

        return "/admin/home";
    }

    @GetMapping(value = "/admin/publishpost/{postId}")
    public String getAdminPublishPost(
            @PathVariable Long postId,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Post post = adminService.GetPostById(postId);
        post.setPostStatus(POSTSTATUS_PUBLICATE);
        post.setApruve(true);
        post.setPublishdate(new Date());
        adminService.SavePost(post);
        historyService.saveHistory(EHistoryType.History_PostPublicate,"Apruve post "+post.getUuid()+" cd="+post.getClientdetail().getId(),post.getClientdetail().getUser());
        return "redirect:/admin/home";
    }

    @GetMapping(value = "/admin/delpost/{postId}")
    public String getAdminDeletePost(
            @PathVariable Long postId,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Post post = adminService.GetPostById(postId);
        post.setPostStatus(POSTSTATUS_DELETE);
        post.setApruve(false);
//        post.setPublishdate(new Date());
        adminService.SavePost(post);
        historyService.saveHistory(EHistoryType.History_PostDelete,"Delete post "+post.getUuid()+" cd="+post.getClientdetail().getId(),post.getClientdetail().getUser());
        return "redirect:/admin/home";
    }


    @GetMapping(value = "/admin/rejectpost/{postId}")
    public String getAdminRejectPost(
            @PathVariable Long postId,
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Post post = adminService.GetPostById(postId);
        post.setPostStatus(POSTSTATUS_REJECT);
        post.setApruve(false);
//        post.setPublishdate(new Date());
        adminService.SavePost(post);
        historyService.saveHistory(EHistoryType.History_PostReject,"Delete post "+post.getUuid()+" cd="+post.getClientdetail().getId(),post.getClientdetail().getUser());
        return "redirect:/admin/home";
    }
}
