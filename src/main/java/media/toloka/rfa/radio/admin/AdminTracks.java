package media.toloka.rfa.radio.admin;

import media.toloka.rfa.radio.admin.service.AdminService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Profile("Front")
@Controller
public class AdminTracks {

    @Autowired
    private AdminService adminService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private EmailSenderService emailSenderService;

    @Value("${media.toloka.rfa.mail.from:info@toloka.media}")
    private String mailFrom;

    final Logger logger = LoggerFactory.getLogger(AdminTracks.class);

    private void sendTrackStatusEmail(Track track, String status) {
        if (track.getClientdetail() == null || track.getClientdetail().getUser() == null) return;
        
        String userEmail = track.getClientdetail().getUser().getEmail();
        if (userEmail == null || userEmail.isEmpty()) return;

        try {
            Mail mail = new Mail();
            mail.setFrom(mailFrom);
            mail.setTo(userEmail);
            mail.setSubject("Статус вашого треку на Радіо Толока");
            
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

    @GetMapping(value = "/admin/tracks")
    public String getAdminTracks(Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        List<Track> tracks = adminService.GetNotApruveTracks();
        model.addAttribute("trackList", tracks);

        return "/admin/tracks";
    }

    @GetMapping(value = "/admin/publishtrack/{trackId}")
    public String getAdminPublishTrack(@PathVariable Long trackId) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Track track = adminService.GetTrackById(trackId);
        if (track != null) {
            track.setApruve(true);
            track.setPublishstatus(true);
            adminService.SaveTrack(track);
            historyService.saveHistory(EHistoryType.History_DocumentCreate, 
                "Admin approved track: " + track.getName() + " (UUID: " + track.getUuid() + ")", 
                track.getClientdetail().getUser());
            
            sendTrackStatusEmail(track, "APPROVED");
        }
        return "redirect:/admin/tracks";
    }

    @GetMapping(value = "/admin/rejecttrack/{trackId}")
    public String getAdminRejectTrack(@PathVariable Long trackId) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Track track = adminService.GetTrackById(trackId);
        if (track != null) {
            track.setApruve(false);
            track.setPublishstatus(false);
            adminService.SaveTrack(track);
            
            sendTrackStatusEmail(track, "REJECTED");
        }
        return "redirect:/admin/tracks";
    }

    @GetMapping(value = "/admin/deltrack/{trackId}")
    public String getAdminDeleteTrack(@PathVariable Long trackId) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Track track = adminService.GetTrackById(trackId);
        if (track != null) {
            String trackName = track.getName();
            String userEmail = track.getClientdetail().getUser().getEmail();
            
            // Відправляємо лист перед видаленням, поки об'єкт існує
            sendTrackStatusEmail(track, "DELETED");
            
            adminService.DeleteTrack(trackId);
            
            historyService.saveHistory(EHistoryType.History_PostDelete, 
                "Admin deleted track: " + trackName, 
                user);
        }
        return "redirect:/admin/tracks";
    }
}
