package media.toloka.rfa.media.messanger.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.media.messanger.model.MessageRoom;
import media.toloka.rfa.media.messanger.service.MessangerService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.radio.repository.StationRepo;
import media.toloka.rfa.security.model.Users;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 🖥️ Контролер для відображення HTML-сторінки панелі модератора.
 * (Не плутати з EfirModeratorController, який є REST API і повертає JSON).
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ModeratorPageController {

    private final StationRepo stationRepo;
    private final ClientService clientService;
    private final MessangerService messangerService;

    @GetMapping("/moderator/efir/{stationUuid}")
    public String openModeratorPage(@PathVariable String stationUuid, Model model) {
        // 1. Перевірка авторизації
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }

        // 2. Перевірка існування станції
        Station station = stationRepo.getStationByUuid(stationUuid);
        if (station == null) {
            log.warn("Спроба доступу до неіснуючої станції: {}", stationUuid);
            return "redirect:/";
        }

        Clientdetail cd = clientService.GetClientDetailByUser(user);

        // 3. Наповнення моделі даними для Thymeleaf
        model.addAttribute("station", station);
        model.addAttribute("stationUuid", stationUuid);
        model.addAttribute("curusername", cd.getCustname() + " " + cd.getCustsurname());
        model.addAttribute("curuuid", cd.getUuid());

        // 4. Завантаження інформації про кімнати (якщо вони вже створені)
        if (station.getRoomuuid() != null) {
            model.addAttribute("mainRoom", messangerService.GetChatRoomByUUID(station.getRoomuuid()));
        }
        if (station.getGreenRoomUuid() != null) {
            model.addAttribute("greenRoom", messangerService.GetChatRoomByUUID(station.getGreenRoomUuid()));
        }
        if (station.getBroadcastRoomUuid() != null) {
            model.addAttribute("broadcastRoom", messangerService.GetChatRoomByUUID(station.getBroadcastRoomUuid()));
        }
        if (station.getStaffRoomUuid() != null) {
            model.addAttribute("staffRoom", messangerService.GetChatRoomByUUID(station.getStaffRoomUuid()));
        }

        log.info("Модератор {} відкрив панель ефіру для станції {}", cd.getUuid(), station.getName());

        // 5. Повертаємо ім'я HTML-шаблону (без розширення .html)
        // Spring автоматично шукає файл у src/main/resources/templates/moderator/efir.html
        return "moderator/efir";
    }
}