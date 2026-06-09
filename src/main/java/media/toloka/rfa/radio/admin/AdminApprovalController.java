package media.toloka.rfa.radio.admin;

import media.toloka.rfa.radio.admin.service.AdminService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.enumerate.EDocumentStatus;
import media.toloka.rfa.security.model.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Arrays;

@Profile("Front")
@Controller
public class AdminApprovalController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private ClientService clientService;

    @GetMapping("/admin/approvals")
    public String listPendingApprovals(Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        model.addAttribute("approvals", adminService.GetPendingApprovals());
        model.addAttribute("docStatuses", Arrays.asList(
                EDocumentStatus.STATUS_LOADED,
                EDocumentStatus.STATUS_REVIEW,
                EDocumentStatus.STATUS_APPROVED,
                EDocumentStatus.STATUS_REJECTED
        ));
        
        return "/admin/approvals";
    }
}
