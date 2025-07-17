package media.toloka.rfa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Date;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class RFAApplication {
	public static void main(String[] args) {
		SpringApplication.run(RFAApplication.class, args);
	}
	// https://www.marcobehler.com/guides/spring-security

	// Вмикаємо винятки, якщо хендлер не знайдено (404)
//	@Bean
//	public DispatcherServlet dispatcherServlet() {
//		DispatcherServlet servlet = new DispatcherServlet();
////		servlet.setThrowExceptionIfNoHandlerFound(true);
//		return servlet;
//	}

//	@ControllerAdvice
//	public static class GlobalExceptionHandler {
//
//		// Обробка 404
//		@ExceptionHandler(NoHandlerFoundException.class)
//		public String handle404(NoHandlerFoundException ex, HttpServletRequest request, Model model) {
//			model.addAttribute("status", 404);
//			model.addAttribute("error", "Сторінку не знайдено");
//			model.addAttribute("msg", ex.getMessage());
//			model.addAttribute("path", request.getRequestURI());
//			model.addAttribute("timestamp", new Date());
//			return "/error/404";  // Шлях до шаблону 404.html
//		}
//
//		// Обробка всіх інших помилок (500)
//		@ExceptionHandler(Exception.class)
//		public String handle500(Exception ex, HttpServletRequest request, Model model) {
//			model.addAttribute("status", 500);
//			model.addAttribute("error", "Внутрішня помилка сервера");
//			model.addAttribute("msg", ex.getMessage());
//			model.addAttribute("path", request.getRequestURI());
//			model.addAttribute("timestamp", new Date());
//			return "/error/500";  // Шлях до шаблону 500.html
//		}
//	}
}


