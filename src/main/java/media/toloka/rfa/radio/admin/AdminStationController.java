package media.toloka.rfa.radio.admin;

import media.toloka.rfa.radio.admin.service.AdminService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.history.service.HistoryService;
import media.toloka.rfa.radio.post.service.PostService;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Profile("Front")
@Controller
public class AdminStationController {

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


    final Logger logger = LoggerFactory.getLogger(AdminStationController.class);


    @GetMapping(value = "/admin/station")
    public String getAdminStation(
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }



        // Документи до опрацювання
//        model.addAttribute("qDocuments", adminService.GetClientsWithNotApruvedDocoments().size() );
//        // Пости до опрацювання
//        model.addAttribute("posts", adminService.GetNotApruvePosts().size() );
//        // користувачі до опрацювання
//        model.addAttribute("usersList", adminService.GetAllUsers().size() );
//        // Адреси до опрацювання
//        model.addAttribute("addressesList", adminService.GetNotApruvedAddresses().size() );

        return "/admin/station";
    }
//
//    @GetMapping(value = "/admin/publishpost/{postId}")
//    public String getAdminPublishPost(
//            @PathVariable Long postId,
//            Model model ) {
//        Users user = clientService.GetCurrentUser();
//        if (user == null) {
//            return "redirect:/";
//        }
//
//        Post post = adminService.GetPostById(postId);
//        post.setPostStatus(POSTSTATUS_PUBLICATE);
//        post.setApruve(true);
//        post.setPublishdate(new Date());
//        adminService.SavePost(post);
//        historyService.saveHistory(EHistoryType.History_PostPublicate,"Apruve post "+post.getUuid()
//                        +" cd="+post.getClientdetail().getId()
//                        +" Client UUID="+post.getClientdetail().getUuid()
//                ,post.getClientdetail().getUser());
//        return "redirect:/admin/home";
//    }
//
//    @GetMapping(value = "/admin/delpost/{postId}")
//    public String getAdminDeletePost(
//            @PathVariable Long postId,
//            Model model ) {
//        Users user = clientService.GetCurrentUser();
//        if (user == null) {
//            return "redirect:/";
//        }
//
//        Post post = adminService.GetPostById(postId);
//        post.setPostStatus(POSTSTATUS_DELETE);
//        post.setApruve(false);
////        post.setPublishdate(new Date());
//        adminService.SavePost(post);
//        historyService.saveHistory(EHistoryType.History_PostDelete,"Delete post "+post.getUuid()
//                        +" cd="+post.getClientdetail().getId()
//                        +" Client UUID="+post.getClientdetail().getUuid()
//                ,post.getClientdetail().getUser());
//        return "redirect:/admin/home";
//    }
//
//
//    @GetMapping(value = "/admin/rejectpost/{postId}")
//    public String getAdminRejectPost(
//            @PathVariable Long postId,
//            Model model ) {
//        Users user = clientService.GetCurrentUser();
//        if (user == null) {
//            return "redirect:/";
//        }
//
//        Post post = adminService.GetPostById(postId);
//        post.setPostStatus(POSTSTATUS_REJECT);
//        post.setApruve(false);
////        post.setPublishdate(new Date());
//        adminService.SavePost(post);
//        historyService.saveHistory(EHistoryType.History_PostReject,"Reject post id="+post.getUuid()
//                +" cd="+post.getClientdetail().getId()
//                +" Client UUID="+post.getClientdetail().getUuid()
//                ,post.getClientdetail().getUser());
//        return "redirect:/admin/home";
//    }
}
