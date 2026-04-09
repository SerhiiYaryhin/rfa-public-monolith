package media.toloka.rfa.radio.admin;

import media.toloka.rfa.radio.admin.service.AdminService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.history.service.HistoryService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Documents;
import media.toloka.rfa.radio.post.service.PostService;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.List;

@Profile("Front")
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
        Users user = clientService.getCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

//        Clientdetail cd = clientService.getClientDetailByUser(clientService.getCurrentUser());
//        List<Post> posts = adminService.getNotApruvePosts();
        // Документи до опрацювання

//        List<Clientdetail> documentsList = adminService.getClientsWithNotApruvedDocoments();
//        List<Documents> documentsList = adminService.GetNotApruvedDocuments();
//        HashMap<Long, Integer> qDocuments = new HashMap<>();
//        for (Documents doc : documentsList) {
//            if (!qDocuments.containsKey(doc.getClientdetail().getId())) {
//                qDocuments.put(doc.getClientdetail().getId(), doc.getClientdetail().getDocumentslist().size());
//            }
//        }
        // тимчасово поточний користувач
        model.addAttribute("curuser", user );
        // Документи до опрацювання
        model.addAttribute("qDocuments", adminService.getClientsWithNotApruvedDocoments().size() );
        // Пости до опрацювання
        model.addAttribute("posts", adminService.getNotApruvePosts().size() );
        // користувачі до опрацювання
        model.addAttribute("usersList", adminService.getAllUsers().size() );
        // Адреси до опрацювання
        model.addAttribute("addressesList", adminService.getNotApruvedAddresses().size() );

        return "/admin/home";
    }
}
