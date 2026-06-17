package media.toloka.rfa.webrtc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestWebRtcController {

    // Спрощений ендпоінт, який поверне сторінку тестування
    @GetMapping("/test/webrtc/webrtc-test")
    public String getTestPage() {
        // Повертає назву шаблону файлу (webrtc-test.html)
        return "/test/webrtc/webrtc-test";
    }
}