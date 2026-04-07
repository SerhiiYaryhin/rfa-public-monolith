package media.toloka.rfa.config;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Date;

/**
 * Кастомний контролер помилок.
 * Перехоплює всі запити на /error і повертає відповідний шаблон
 * залежно від HTTP статус-коду.
 */
@Controller
public class MyErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        // Отримуємо атрибути помилки
        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        // Парсимо статус-код
        int status = 500;
        if (statusCode != null) {
            try {
                status = Integer.parseInt(statusCode.toString());
            } catch (NumberFormatException e) {
                status = 500;
            }
        }

        // Формуємо повідомлення
        String msg = message != null ? message.toString() : "Невідома помилка";
        if (exception != null && exception instanceof Throwable) {
            msg = ((Throwable) exception).getMessage();
        }

        String path = uri != null ? uri.toString() : "невідомо";

        // Заповнюємо модель
        model.addAttribute("timestamp", new Date());
        model.addAttribute("status", status);
        model.addAttribute("error", getErrorDescription(status));
        model.addAttribute("message", msg);
        model.addAttribute("path", path);

        // Повертаємо відповідний шаблон
        return switch (status) {
            case 404 -> "/error/404";
            case 403 -> "/error/403";
            case 500 -> "/error/500";
            default -> "/error/error";
        };
    }

    /**
     * Повертає зрозумілий опис помилки українською.
     */
    private String getErrorDescription(int status) {
        return switch (status) {
            case 400 -> "Невірний запит";
            case 401 -> "Неавторизований доступ";
            case 403 -> "Доступ заборонено";
            case 404 -> "Сторінку не знайдено";
            case 405 -> "Метод не підтримується";
            case 408 -> "Таймаут запиту";
            case 429 -> "Забагато запитів";
            case 500 -> "Внутрішня помилка сервера";
            case 502 -> "Невірний шлюз";
            case 503 -> "Сервер тимчасово недоступний";
            case 504 -> "Таймаут шлюзу";
            default -> "Сталася помилка";
        };
    }
}
