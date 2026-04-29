package media.toloka.rfa.config;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Date;

@Controller
public class MyErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object uriObj = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        Object exceptionObj = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        int status = (statusCode != null) ? Integer.parseInt(statusCode.toString()) : 500;
        String path = (uriObj != null) ? uriObj.toString() : "невідомо";
        
        String errorTitle = switch (status) {
            case 404 -> "Сторінку не знайдено";
            case 403 -> "Доступ обмежено";
            case 400 -> "Некоректний запит";
            default -> "Упс! Щось пішло не так";
        };

        String errorDescription = switch (status) {
            case 404 -> "Схоже, ми не можемо знайти сторінку, яку ви шукаєте. Можливо, вона була видалена або перенесена.";
            case 403 -> "Вибачте, але у вас недостатньо прав для перегляду цієї сторінки.";
            default -> "Сталася внутрішня помилка сервера. Наші технічні фахівці вже працюють над її усуненням.";
        };

        model.addAttribute("timestamp", new Date());
        model.addAttribute("status", status);
        model.addAttribute("title", errorTitle);
        model.addAttribute("description", errorDescription);
        model.addAttribute("path", path);

        if (exceptionObj instanceof Throwable) {
            model.addAttribute("msg", ((Throwable) exceptionObj).getMessage());
        }

        return "/error/error";
    }
}
