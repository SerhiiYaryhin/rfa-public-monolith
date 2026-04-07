package media.toloka.rfa.config;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * Контролер для відображення кастомних сторінок помилок.
 * Викликається через ErrorPageConfig (ErrorPageRegistrar).
 */
@Controller
@RequestMapping("/error")
public class CustomErrorPageController {

    @GetMapping("/404-page")
    public String error404(HttpServletRequest request, Model model) {
        model.addAttribute("status", 404);
        model.addAttribute("error", "Сторінку не знайдено");
        model.addAttribute("path", request.getRequestURI());
        model.addAttribute("timestamp", new Date());
        return "/error/404";
    }

    @GetMapping("/403-page")
    public String error403(HttpServletRequest request, Model model) {
        model.addAttribute("status", 403);
        model.addAttribute("error", "Доступ заборонено");
        model.addAttribute("path", request.getRequestURI());
        model.addAttribute("timestamp", new Date());
        return "/error/403";
    }

    @GetMapping("/500-page")
    public String error500(HttpServletRequest request, Model model) {
        model.addAttribute("status", 500);
        model.addAttribute("error", "Внутрішня помилка сервера");
        model.addAttribute("path", request.getRequestURI());
        model.addAttribute("timestamp", new Date());
        return "/error/500";
    }

    @GetMapping("/generic-page")
    public String errorGeneric(HttpServletRequest request, Model model) {
        Object statusCode = request.getAttribute("jakarta.servlet.error.status_code");
        Object message = request.getAttribute("jakarta.servlet.error.message");

        int status = 500;
        if (statusCode != null) {
            try {
                status = Integer.parseInt(statusCode.toString());
            } catch (NumberFormatException e) {
                status = 500;
            }
        }

        model.addAttribute("status", status);
        model.addAttribute("error", message != null ? message.toString() : "Сталася помилка");
        model.addAttribute("path", request.getRequestURI());
        model.addAttribute("timestamp", new Date());
        return "/error/error";
    }
}
