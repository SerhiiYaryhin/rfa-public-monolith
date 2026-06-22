package media.toloka.rfa.media.messanger.webrtc.controller;

import media.toloka.rfa.media.messanger.model.MessageRoom;
import media.toloka.rfa.media.messanger.model.enumerate.EChatRoomType;
import media.toloka.rfa.media.messanger.service.MessangerService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Контролер для обслуговування сторінок тестування WebRTC.
 *
 * При запуску застосунку потрібно видалити всі кімнати в Janus і створити кімнати,
 * спираючись на радіостанції, які відкрили кімнати для прямого ефіру.
 *
 * На відміну від JanusLiveController, цей клас є звичайним @Controller,
 * що повертає HTML шаблони (Thymeleaf), а не JSON дані.
 */
@Controller
public class TestWebRtcController {

    @Autowired
    private ClientService clientService;
    @Autowired
    private MessangerService messangerService;

    private static final Logger logger = LoggerFactory.getLogger(TestWebRtcController.class);

    /**
     * Повертає сторінку для інтерактивного тестування WebRTC ефіру.
     * Сторінка розташована в: src/main/resources/templates/test/webrtc/webrtc-test.html
     */
    @GetMapping("/test/webrtc/webrtc-test")
    public String getTestPage(
            Model model ) {

        Users user = clientService.GetCurrentUser();
        if (user == null) { return "redirect:/login"; }
        Clientdetail cd = clientService.GetClientDetailByUser(user);
        logger.info("Зайшли на сторінку webrtc-test. UUID користувача: {}", cd.getUuid());
        List<MessageRoom> liveRoom = messangerService.GetLiveChatRoomList(EChatRoomType.CHATROOM_TYPE_STATION);

        // передаємо параметри на сторінку
        model.addAttribute("clientdetail", clientService.GetClientDetailByUser(user));
        model.addAttribute("roomlist", liveRoom );

        return "/test/webrtc/webrtc-test";
    }
}
