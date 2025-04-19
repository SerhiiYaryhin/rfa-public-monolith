package media.toloka.rfa.config;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Date;

//
//@ControllerAdvice
//public class MyErrorController implements ErrorController {
//
//    @RequestMapping("/error")
//    @ExceptionHandler(Exception.class)
////    @ExceptionHandler(NoHandlerFoundException.class)
//
////    public String handleError(HttpServletRequest request, Model model) {
//    public String handleError(HttpServletRequest request, Model model) {
//        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
//        Object exception1 = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
//
//        Throwable exception = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
//        String message = exception != null ? exception.getMessage() : "Невідома помилка";
//        String requestURI =request.getRequestURI();
//
//        model.addAttribute("msg", message);
//
//        model.addAttribute("status", status);
//        model.addAttribute("error", "Сталася помилка");
//        model.addAttribute("path", requestURI);
////        model.addAttribute("msg", exception.getMessage());
//        model.addAttribute("timestamp", new Date());
//
////        if (exception != null) {
////            model.addAttribute("trace", ((Throwable) exception).getStackTrace());
////        }
//
////        return "/error/rfaerror"; // Повертаємо ім'я шаблону (404.html)
//        return "/error/templateserror"; // Повертаємо ім'я шаблону (404.html)
//    }
//}
