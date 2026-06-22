package media.toloka.rfa.webrtc.controller;

import media.toloka.rfa.media.messanger.model.MessageRoom;
import media.toloka.rfa.media.messanger.model.enumerate.EChatRoomType;
import media.toloka.rfa.media.messanger.service.MessangerService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.security.model.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Контролер для обслуговування сторінок тестування WebRTC.
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
        List<MessageRoom> liveRoom = messangerService.GetLiveChatRoomList(EChatRoomType.CHATROOM_TYPE_STATION);

        // пердаємо параметри на сторінку
        model.addAttribute("clientdetail", clientService.GetClientDetailByUser(user));
        model.addAttribute("roomlist", liveRoom );

        return "/test/webrtc/webrtc-test";
    }
}
