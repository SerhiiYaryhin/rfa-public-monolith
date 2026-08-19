package media.toloka.rfa.radio.admin;

import media.toloka.rfa.radio.admin.service.AdminService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.email.service.EmailSenderService;
import media.toloka.rfa.radio.history.service.HistoryService;
import media.toloka.rfa.radio.model.Album;
import media.toloka.rfa.radio.model.Mail;
import media.toloka.rfa.radio.model.Track;
import media.toloka.rfa.radio.model.enumerate.EAlbumStatus;
import media.toloka.rfa.radio.model.enumerate.EHistoryType;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Profile("Front")
@Controller
public class AdminAlbums {

    @Autowired
    private AdminService adminService;

    @Autowired
    private CreaterService createrService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private EmailSenderService emailSenderService;

    @Value("${media.toloka.rfa.mail.from:info@toloka.media}")
    private String mailFrom;

    final Logger logger = LoggerFactory.getLogger(AdminAlbums.class);

    /**
     * Відображення списку всіх альбомів для адміністратора/модератора
     */
    @GetMapping(value = "/admin/albums")
    public String getAdminAlbums(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        org.springframework.data.domain.Page<Album> albumPage = createrService.GetAllAlbumsForAdmin(page, 15);
        model.addAttribute("albumList", albumPage.getContent());
        model.addAttribute("totalPages", albumPage.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("linkPage", "/admin/albums");
        return "/admin/albums";
    }

    /**
     * Схвалення альбому для публікації
     */
    @GetMapping(value = "/admin/approvealbum/{albumUuid}")
    public String approveAlbum(@PathVariable String albumUuid) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        Album album = createrService.GetAlbumByUuid(albumUuid);
        if (album != null) {
            album.setStatus(EAlbumStatus.APPROVED);
            album.setPublished(true); // Автоматично публікуємо при схваленні
            createrService.SaveAlbum(album);

            historyService.saveHistory(EHistoryType.History_DocumentCreate,
                    "Admin approved album: " + album.getName() + " (UUID: " + album.getUuid() + ")",
                    user);

            sendAlbumStatusEmail(album, "APPROVED");
            logger.info("Admin approved album: {}", album.getUuid());
        }
        return "redirect:/admin/albums";
    }

    /**
     * Відхилення альбому
     */
    @GetMapping(value = "/admin/rejectalbum/{albumUuid}")
    public String rejectAlbum(@PathVariable String albumUuid) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        Album album = createrService.GetAlbumByUuid(albumUuid);
        if (album != null) {
            album.setStatus(EAlbumStatus.REJECTED);
            album.setPublished(false); // Знімаємо з публікації при відхиленні
            createrService.SaveAlbum(album);

            historyService.saveHistory(EHistoryType.History_DocumentChange,
                    "Admin rejected album: " + album.getName() + " (UUID: " + album.getUuid() + ")",
                    user);

            sendAlbumStatusEmail(album, "REJECTED");
            logger.info("Admin rejected album: {}", album.getUuid());
        }
        return "redirect:/admin/albums";
    }

    /**
     * Перемикання статусу публікації (без зміни статусу модерації)
     */
    @GetMapping(value = "/admin/togglepublishalbum/{albumUuid}")
    public String togglePublishAlbum(@PathVariable String albumUuid) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        Album album = createrService.GetAlbumByUuid(albumUuid);
        if (album != null) {
            boolean newPublishedState = !album.getPublished();
            album.setPublished(newPublishedState);
            createrService.SaveAlbum(album);

            historyService.saveHistory(EHistoryType.History_DocumentChange,
                    "Admin toggled album publication to " + newPublishedState + ": " + album.getName() + " (UUID: " + album.getUuid() + ")",
                    user);

            logger.info("Admin toggled album publication: {} -> {}", album.getUuid(), newPublishedState);
        }
        return "redirect:/admin/albums";
    }

    /**
     * Повне видалення альбому
     */
    @GetMapping(value = "/admin/delalbum/{albumUuid}")
    public String deleteAlbum(@PathVariable String albumUuid) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        Album album = createrService.GetAlbumByUuid(albumUuid);
        if (album != null) {
            String albumName = album.getName();
            createrService.DeleteAlbum(album);

            historyService.saveHistory(EHistoryType.History_PostDelete,
                    "Admin deleted album: " + albumName + " (UUID: " + albumUuid + ")",
                    user);

            sendAlbumStatusEmail(album, "DELETED");
            logger.info("Admin deleted album: {}", albumUuid);
        }
        return "redirect:/admin/albums";
    }

    /**
     * Сторінка редагування альбому адміністратором
     */
    @GetMapping(value = "/admin/editalbum/{albumUuid}")
    public String adminEditAlbum(@PathVariable String albumUuid, Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        Album album = createrService.GetAlbumByUuid(albumUuid);
        if (album == null) return "redirect:/admin/albums";

        List<Track> tracks = album.getTrack();

        model.addAttribute("album", album);
        model.addAttribute("trackList", tracks);
        model.addAttribute("statusOptions", EAlbumStatus.values());
        return "/admin/editalbum";
    }

    /**
     * Збереження відредагованого альбому адміністратором
     */
    @PostMapping(value = "/admin/editalbum")
    public String adminSaveAlbum(@ModelAttribute Album falbum) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        Album album = createrService.GetAlbumByUuid(falbum.getUuid());
        if (album != null) {
            EAlbumStatus oldStatus = album.getStatus();
            boolean oldPublished = album.getPublished();

            album.setName(falbum.getName());
            album.setAutor(falbum.getAutor());
            album.setStyle(falbum.getStyle());
            album.setDescription(falbum.getDescription());
            album.setStatus(falbum.getStatus());
            album.setPublished(falbum.getPublished());

            createrService.SaveAlbum(album);
            logger.info("Admin updated album: {}", album.getUuid());

            // Якщо статус змінився, відправляємо сповіщення
            if (oldStatus != album.getStatus()) {
                String statusString = album.getStatus() == EAlbumStatus.APPROVED ? "APPROVED" :
                        (album.getStatus() == EAlbumStatus.REJECTED ? "REJECTED" : "PENDING");
                sendAlbumStatusEmail(album, statusString);
            }

            historyService.saveHistory(EHistoryType.History_DocumentChange,
                    "Admin updated album: " + album.getName() + " (UUID: " + album.getUuid() + ")",
                    user);
        }
        return "redirect:/admin/albums";
    }

    /**
     * Відправка email сповіщення автору про зміну статусу альбому
     */
    private void sendAlbumStatusEmail(Album album, String status) {
        if (album.getClientdetail() == null || album.getClientdetail().getUser() == null) return;
        String userEmail = album.getClientdetail().getUser().getEmail();
        if (userEmail == null || userEmail.isEmpty()) return;

        try {
            Mail mail = new Mail();
            mail.setFrom(mailFrom);
            mail.setTo(userEmail);
            mail.setSubject("Статус вашого альбому на Радіо Толока");

            Map<String, Object> model = new HashMap<>();
            model.put("userName", album.getClientdetail().getCustname());
            model.put("albumName", album.getName());
            model.put("status", status);

            mail.setHtmlTemplate(new Mail.HtmlTemplate("mail/albumStatusNotification", model));
            emailSenderService.sendEmail(mail);
        } catch (Exception e) {
            logger.error("Failed to send album status email to {}: {}", userEmail, e.getMessage());
        }
    }
}