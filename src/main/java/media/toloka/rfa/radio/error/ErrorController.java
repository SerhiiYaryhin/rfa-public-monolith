package media.toloka.rfa.radio.error;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ErrorController {

    @GetMapping("/error/general")
    public String generalError() {
        return "/error/general";
    }
}