package media.toloka.rfa.webrtc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Контролер для обслуговування сторінок тестування WebRTC.
 * 
 * На відміну від JanusLiveController, цей клас є звичайним @Controller,
 * що повертає HTML шаблони (Thymeleaf), а не JSON дані.
 */
@Controller
public class TestWebRtcController {

    /**
     * Повертає сторінку для інтерактивного тестування WebRTC ефіру.
     * Сторінка розташована в: src/main/resources/templates/test/webrtc/webrtc-test.html
     */
    @GetMapping("/test/webrtc/webrtc-test")
    public String getTestPage() {
        // Назва файлу шаблону без розширення .html
        return "/test/webrtc/webrtc-test";
    }
}
