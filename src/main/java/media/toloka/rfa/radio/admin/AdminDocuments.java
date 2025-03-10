package media.toloka.rfa.radio.admin;


import media.toloka.rfa.radio.admin.service.AdminService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.document.service.DocumentService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Documents;
import media.toloka.rfa.radio.model.enumerate.EDocumentStatus;
import media.toloka.rfa.security.model.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static media.toloka.rfa.radio.model.enumerate.EDocumentStatus.*;

@Profile("Front")
@Controller
public class AdminDocuments {

    @Autowired
    private AdminService adminService;
    @Autowired
    private ClientService clientService;
    @Autowired
    private DocumentService documentService;

    @GetMapping(value = "/admin/documents")
    public String getUserHome(
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        List<Documents> documentsList = adminService.GetNotApruvedDocuments();
        HashMap<Long, Integer> clientdetailLongHashMap = new HashMap<>();
        List<Clientdetail> clientdetailList =  new ArrayList<>();
        for (Documents doc : documentsList) {
            if (!clientdetailLongHashMap.containsKey(doc.getClientdetail().getId())) {
                Clientdetail cd = doc.getClientdetail();
                Integer sz = cd.getDocumentslist().size();
                clientdetailList.add(cd);
                clientdetailLongHashMap.put(cd.getId(), sz);
            }
        }
        model.addAttribute("clientdetailList", clientdetailList );
        return "/admin/documents";
    }

    @GetMapping("/admin/documentedit/{idDocument}")
    public String GetDocumentEdit(
            @PathVariable Long idDocument,
//            IDDocuments document,
            Model model
    ) {
        // Витягуєм користувача
        Users user = clientService.GetCurrentUser();
        if (user == null) { return "redirect:/"; }
        // витягуємо документ, який ми будемо редагувати і передаємо в форму
        Documents doc = documentService.GetDocument(idDocument);
        List<EDocumentStatus> options = new ArrayList<EDocumentStatus>();
        options.add(STATUS_UNKNOWN);
        options.add(STATUS_LOADED);
        options.add(STATUS_REVIEW);
        options.add(STATUS_REJECTED);
        options.add(STATUS_APPROVED);

        model.addAttribute("options", options);
        model.addAttribute("document", doc);
        model.addAttribute("ide", idDocument);
        model.addAttribute("user", user);
        return ("/admin/documentedit");
    }

    @PostMapping("/admin/documentedit")
    public String PostDocumentEdit(
//            @ModelAttribute Long ide,
            @ModelAttribute Documents document,
//            @ModelAttribute User user,
            Model model
    ) {
//        Authentication au;
//        au = SecurityContextHolder.getContext().getAuthentication();
//        Users user = clientService.getByEmail(au.getName());
        Users user = clientService.GetCurrentUser();
        // Якщо не залогінені, то переходимо на головну.
        if (user == null) {
            return "redirect:/";
        }
        Documents curDocument = documentService.GetDocument(document.getId());
//        if (cur)
//        curDocument.setDocumentstatus(document.getDocumentstatus());
        curDocument.setUserComment(document.getUserComment());
        curDocument.setDocumenttype(document.getDocumenttype());
        // TODO Подумати, чи варто тут робити обробку зміни статусу у залежності від ролі користувача
//        curDocument.setStatus(document.getStatus());
        curDocument.setStatus(document.getStatus());
        curDocument.setAdminComment(document.getAdminComment());

        documentService.saveDocument(curDocument);


//        model.addAttribute("user", user);
        return ("redirect:/admin/documents");
    }



}
