package media.toloka.rfa.radio.admin;

import media.toloka.rfa.radio.admin.service.AdminService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.email.service.EmailSenderService;
import media.toloka.rfa.radio.model.EmailDraft;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * Універсальний контролер для розсилки листів.
 * Доступний для ролей: Адмін, Редактор, Модератор.
 */
@Profile("Front")
@Controller
@RequestMapping("/admin/email")
public class AdminEmailController {

    private final Logger logger = LoggerFactory.getLogger(AdminEmailController.class);

    @Autowired
    private AdminService adminService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private EmailSenderService emailSenderService;

    /**
     * Відображення головної сторінки поштового хабу.
     * @param recipient Опційна адреса (для індивідуального надсилання)
     * @param template Опційне ім'я шаблону для завантаження
     */
    @GetMapping("/tool")
    public String showEmailTool(
            @RequestParam(required = false) String recipient,
            @RequestParam(required = false) String template,
            Model model
    ) {
        Users currentUser = clientService.GetCurrentUser();
        if (currentUser == null) return "redirect:/";

        EmailDraft draft = new EmailDraft();
        if (recipient != null) draft.setRecipients(Collections.singletonList(recipient));
        if (template != null) {
            draft.setSelectedTemplate(template);
            draft.setBody(emailSenderService.getTemplateRawContent(template));
        }

        // Список усіх користувачів для вибору отримувачів
        model.addAttribute("users", adminService.GetAllUsers());
        // Список доступних шаблонів у системі
        model.addAttribute("templates", emailSenderService.getAvailableTemplates());
        model.addAttribute("draft", draft);

        return "/admin/email_sender";
    }

    /**
     * AJAX-ендпоінт для отримання контенту шаблону.
     */
    @GetMapping("/template-content")
    @ResponseBody
    public String getTemplateContent(@RequestParam String name) {
        return emailSenderService.getTemplateRawContent(name);
    }

    /**
     * Обробка відправки листів.
     */
    @PostMapping("/send")
    public String processSend(@ModelAttribute EmailDraft draft, Model model) {
        if (draft.getRecipients() == null || draft.getRecipients().isEmpty()) {
            model.addAttribute("error", "Отримувачів не обрано!");
            return showEmailTool(null, null, model);
        }

        logger.info("Початок розсилки на {} адрес", draft.getRecipients().size());
        
        for (String email : draft.getRecipients()) {
            emailSenderService.sendRawHtmlEmail(email, draft.getSubject(), draft.getBody());
        }

        model.addAttribute("message", "Листи успішно відправлено!");
        return "redirect:/admin/email/tool?success=true";
    }
}
