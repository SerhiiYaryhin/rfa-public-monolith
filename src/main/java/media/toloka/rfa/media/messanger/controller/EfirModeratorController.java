package media.toloka.rfa.media.messanger.controller;

import media.toloka.rfa.media.messanger.model.MessageRoom;
import media.toloka.rfa.media.messanger.service.MessangerService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.radio.repository.StationRepo;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class EfirModeratorController {

    private static final Logger logger = LoggerFactory.getLogger(EfirModeratorController.class);

    @Autowired
    private ClientService clientService;

    @Autowired
    private StationRepo stationRepo;

    @Autowired
    private MessangerService messangerService;

    /**
     * Сторінка модератора прямого ефіру.
     * Доступна тільки для авторизованих користувачів.
     */
    @GetMapping("/moderator/efir/{stationUuid}")
    public String efirModeratorPage(@PathVariable String stationUuid, Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }

        Station station = stationRepo.getStationByUuid(stationUuid);
        if (station == null) {
            logger.warn("Станцію з UUID {} не знайдено", stationUuid);
            return "redirect:/";
        }

        // Перевірка прав: тільки власник або модератор станції
        // TODO: Додати перевірку через StationRoleProvider
        Clientdetail cd = clientService.GetClientDetailByUser(user);
//        if (cd == null || !cd.getUuid().equals(station.getClientdetail().getUuid())) {
        if (cd == null ) {
            logger.warn("Користувач {} не має прав на станцію {}", user.getEmail(), stationUuid);
            return "redirect:/";
        }

        // Додаємо дані в модель
        model.addAttribute("user", user);
        model.addAttribute("clientdetail", cd);
        model.addAttribute("station", station);
        model.addAttribute("stationUuid", stationUuid);
        model.addAttribute("curuuid", cd.getUuid());
        model.addAttribute("curusername", cd.getCustname() + " " + cd.getCustsurname());

        // Додаємо існуючу головну кімнату станції (якщо є)
        if (station.getRoomuuid() != null) {
            MessageRoom mainRoom = messangerService.GetChatRoomByUUID(station.getRoomuuid());
            model.addAttribute("mainRoom", mainRoom);
        }

        // Додаємо кімнати прямого ефіру (якщо вже створені)
        if (station.getGreenRoomUuid() != null) {
            model.addAttribute("greenRoom", messangerService.GetChatRoomByUUID(station.getGreenRoomUuid()));
        }
        if (station.getBroadcastRoomUuid() != null) {
            model.addAttribute("broadcastRoom", messangerService.GetChatRoomByUUID(station.getBroadcastRoomUuid()));
        }
        if (station.getStaffRoomUuid() != null) {
            model.addAttribute("staffRoom", messangerService.GetChatRoomByUUID(station.getStaffRoomUuid()));
        }

        logger.info("Модератор {} відкрив сторінку ефіру для станції {}", user.getEmail(), station.getName());
        return "/moderator/efir";
    }
}