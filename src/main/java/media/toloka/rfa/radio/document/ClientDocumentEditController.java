package media.toloka.rfa.radio.document;

import lombok.extern.slf4j.Slf4j;

import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Documents;
import media.toloka.rfa.radio.model.enumerate.EDocumentStatus;
import media.toloka.rfa.radio.document.service.DocumentService;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.List;

import media.toloka.rfa.radio.model.Documents;

import static media.toloka.rfa.radio.model.enumerate.EDocumentStatus.*;

@Profile("Front")
@Slf4j
@Controller
public class ClientDocumentEditController {

    @Autowired
    private ClientService clientService;

    @Autowired
    private DocumentService documentService;

    final Logger logger = LoggerFactory.getLogger(ClientDocumentEditController.class);

    @GetMapping("/user/documentedit/{uuidDocument}")
    public String GetDocumentEdit(
//            @PathVariable Long idDocument,
            @PathVariable String uuidDocument,
//            IDDocuments document,
            Model model
    ) {
        // Витягуєм користувача
        Users user = clientService.GetCurrentUser();
        if (user == null) { return "redirect:/"; }
        // витягуємо документ, який ми будемо редагувати і передаємо в форму
//        Documents doc = documentService.GetDocument(idDocument);
        Documents doc = documentService.GetDocumentByUUID(uuidDocument);
        List<EDocumentStatus> options = new ArrayList<EDocumentStatus>();
        options.add(STATUS_UNKNOWN);
        options.add(STATUS_LOADED);
        options.add(STATUS_REVIEW);
        options.add(STATUS_APPROVED);
        options.add(STATUS_REJECTED);

        model.addAttribute("options", options);
        model.addAttribute("document", doc);
//        model.addAttribute("ide", idDocument);
        model.addAttribute("user", user);
        return ("/user/documentedit");
    }
    @PostMapping("/user/documentedit")
    public String PostDocumentEdit(
//            @ModelAttribute Long ide,
            @ModelAttribute Documents document,
//            @ModelAttribute User user,
            Model model
    ) {
        Authentication au;
        au = SecurityContextHolder.getContext().getAuthentication();
//        Users user = clientService.getByEmail(au.getName());
        Users user = clientService.GetCurrentUser();
        // Якщо не залогінені, то переходимо на головну.
        if (user == null) {
            return "redirect:/";
        }
        Documents curDocument = documentService.GetDocumentByUUID(document.getUuid()); //GetDocument(document.getId());
//        if (cur)
//        curDocument.setDocumentstatus(document.getDocumentstatus());
        curDocument.setUserComment(document.getUserComment());
        curDocument.setDocumenttype(document.getDocumenttype());
        // TODO Подумати, чи варто тут робити обробку зміни статусу у залежності від ролі користувача
//        curDocument.setStatus(document.getStatus());
        curDocument.setStatus(STATUS_UNKNOWN);

        documentService.saveDocument(curDocument);


        model.addAttribute("user", user);
        return ("redirect:/user/documents");
    }
}
