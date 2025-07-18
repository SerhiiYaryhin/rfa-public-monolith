package media.toloka.rfa.config;
//
//import jakarta.servlet.RequestDispatcher;
//import jakarta.servlet.http.HttpServletRequest;
//import org.springframework.boot.web.servlet.error.ErrorController;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.RequestMapping;
//
//import java.util.Date;
//
//@Controller
//public class MyErrorController implements ErrorController {
//
//    @RequestMapping("/error")
//    public String handleError(HttpServletRequest request, Model model) {
//        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
//        Object exceptionObj = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
//        Object uriObj = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
//
//        String path = uriObj != null ? uriObj.toString() : "невідомо";
//        int status = statusCode != null ? Integer.parseInt(statusCode.toString()) : 500;
//        Throwable exception = exceptionObj instanceof Throwable ? (Throwable) exceptionObj : null;
//
//        String message = exception != null ? exception.getMessage() : "Невідома помилка";
//
//        model.addAttribute("timestamp", new Date());
//        model.addAttribute("status", status);
//        model.addAttribute("error", switch (status) {
//            case 404 -> "Сторінку не знайдено";
//            case 403 -> "Доступ заборонено";
//            case 500 -> "Внутрішня помилка сервера";
//            default -> "Невідома помилка";
//        });
//        model.addAttribute("msg", message);
//        model.addAttribute("path", path);
//
//        // Обираємо відповідний шаблон
//        return switch (status) {
//            case 404 -> "/error/404";
//            case 403 -> "/error/403";
//            default -> "/error/500";
//        };
//    }
//}
