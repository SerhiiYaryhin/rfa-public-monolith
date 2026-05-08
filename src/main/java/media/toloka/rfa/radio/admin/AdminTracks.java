package media.toloka.rfa.radio.admin;

import media.toloka.rfa.radio.admin.service.AdminService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.history.service.HistoryService;
import media.toloka.rfa.radio.email.service.EmailSenderService;
import media.toloka.rfa.radio.model.Mail;
import media.toloka.rfa.radio.model.Track;
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
public class AdminTracks {

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

    final Logger logger = LoggerFactory.getLogger(AdminTracks.class);

    /**
     * Відправляє авторський лист про зміну статусу треку.
     * @param track Об'єкт треку
     * @param status Рядок статусу для шаблону (APPROVED, REJECTED, DELETED)
     */
    private void sendTrackStatusEmail(Track track, String status) {
        if (track.getClientdetail() == null || track.getClientdetail().getUser() == null) return;
        
        String userEmail = track.getClientdetail().getUser().getEmail();
        if (userEmail == null || userEmail.isEmpty()) return;

        try {
            Mail mail = new Mail();
            mail.setFrom(mailFrom);
            mail.setTo(userEmail);
            mail.setSubject("Статус вашого треку на Радіо Толока");
            
            // Підготовка даних для Thymeleaf-шаблону листа
            Map<String, Object> model = new HashMap<>();
            model.put("userName", track.getClientdetail().getCustname());
            model.put("trackName", track.getName());
            model.put("status", status);
            
            mail.setHtmlTemplate(new Mail.HtmlTemplate("mail/trackStatusNotification", model));
            emailSenderService.sendEmail(mail);
        } catch (Exception e) {
            logger.error("Failed to send track status email to {}: {}", userEmail, e.getMessage());
        }
    }

    /** Відображення списку всіх треків для адміністратора */
    @GetMapping(value = "/admin/tracks")
    public String getAdminTracks(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        // Отримуємо пагінований список усіх треків
        org.springframework.data.domain.Page<Track> trackPage = createrService.GetTrackPage(page, 15);

        model.addAttribute("trackList", trackPage.getContent());
        model.addAttribute("totalPages", trackPage.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("linkPage", "/admin/tracks");

        return "/admin/tracks";
    }

    @GetMapping(value = "/admin/tracks/{page}")
    public String getAdminTracksOld(@PathVariable Integer page) {
        return "redirect:/admin/tracks?page=" + page;
    }

    /** Перемикання лише статусу публікації на порталі */
    @GetMapping(value = "/admin/togglepublish/{trackUuid}")
    public String togglePublishStatus(@PathVariable String trackUuid) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        Track track = createrService.GetTrackByUuid(trackUuid);
        if (track != null) {
            track.setPublishstatus(!track.getPublishstatus());
            createrService.SaveTrack(track);
        }
        return "redirect:/admin/tracks";
    }

    /** Перемикання статусу публікації треку (Toggle) */
    @GetMapping(value = "/admin/toggletrack/{trackUuid}")
    public String toggleTrackStatus(@PathVariable String trackUuid) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Track track = createrService.GetTrackByUuid(trackUuid);
        if (track != null) {
            boolean newState = !track.getApruve();
            track.setApruve(newState);
            track.setPublishstatus(newState);
            adminService.SaveTrack(track);
            
            // Повідомляємо автора
            String statusString = newState ? "APPROVED" : "REJECTED";
            sendTrackStatusEmail(track, statusString);
            
            historyService.saveHistory(EHistoryType.History_DocumentChange, 
                "Admin toggled track status to " + newState + ": " + track.getName(), 
                track.getClientdetail().getUser());
        }
        return "redirect:/admin/tracks";
    }

    /** Схвалення треку для публікації */
    @GetMapping(value = "/admin/publishtrack/{trackUuid}")
    public String getAdminPublishTrack(@PathVariable String trackUuid) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Track track = createrService.GetTrackByUuid(trackUuid);
        if (track != null) {
            track.setApruve(true);
            track.setPublishstatus(true);
            adminService.SaveTrack(track);
            
            // Реєстрація дії в системному журналі
            historyService.saveHistory(EHistoryType.History_DocumentCreate, 
                "Admin approved track: " + track.getName() + " (UUID: " + track.getUuid() + ")", 
                track.getClientdetail().getUser());
            
            sendTrackStatusEmail(track, "APPROVED");
        }
        return "redirect:/admin/tracks";
    }

    /** Відхилення публікації треку */
    @GetMapping(value = "/admin/rejecttrack/{trackUuid}")
    public String getAdminRejectTrack(@PathVariable String trackUuid) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Track track = createrService.GetTrackByUuid(trackUuid);
        if (track != null) {
            track.setApruve(false);
            track.setPublishstatus(false);
            adminService.SaveTrack(track);
            
            sendTrackStatusEmail(track, "REJECTED");
        }
        return "redirect:/admin/tracks";
    }

    /** Повне видалення треку (файл + записи в БД) */
    @GetMapping(value = "/admin/deltrack/{trackUuid}")
    public String getAdminDeleteTrack(@PathVariable String trackUuid) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Track track = createrService.GetTrackByUuid(trackUuid);
        if (track != null) {
            String trackName = track.getName();
            
            // Повідомляємо автора перед видаленням об'єкта
            sendTrackStatusEmail(track, "DELETED");
            
            // Каскадне видалення через сервіс
            adminService.DeleteTrackByUuid(trackUuid);
            
            historyService.saveHistory(EHistoryType.History_PostDelete, 
                "Admin deleted track: " + trackName, 
                user);
        }
        return "redirect:/admin/tracks";
    }

    /** Сторінка редагування треку адміністратором */
    @GetMapping(value = "/admin/edittrack/{trackUuid}")
    public String adminEditTrack(@PathVariable String trackUuid, Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        Track track = createrService.GetTrackByUuid(trackUuid);
        if (track == null) return "redirect:/admin/tracks";

        model.addAttribute("track", track);
        model.addAttribute("albumList", createrService.GetAllAlbumsByCreater(track.getClientdetail()));
        
        return "/admin/edittrack";
    }

    /** Збереження відредагованого треку адміністратором */
    @PostMapping(value = "/admin/edittrack")
    public String adminSaveTrack(@ModelAttribute Track ftrack) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";

        Track track = createrService.GetTrackByUuid(ftrack.getUuid());
        if (track != null) {
            track.setName(ftrack.getName());
            track.setAutor(ftrack.getAutor());
            track.setStyle(ftrack.getStyle());
            track.setDescription(ftrack.getDescription());
            track.setAlbum(ftrack.getAlbum());
            track.setNotnormalvocabulary(ftrack.getNotnormalvocabulary());
            
            createrService.SaveTrack(track);
            
            logger.info("Admin updated track: {}", track.getUuid());
        }
        return "redirect:/admin/tracks";
    }
}
