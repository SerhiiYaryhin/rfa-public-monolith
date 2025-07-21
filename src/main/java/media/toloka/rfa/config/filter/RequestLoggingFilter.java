package media.toloka.rfa.config.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component; // Важливо для автоматичного виявлення Spring

import java.io.IOException;

@Component // Робить цей фільтр Spring-біном, щоб він був автоматично зареєстрований
public class RequestLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Ініціалізація фільтра (можна залишити порожнім, якщо немає спеціальної логіки)
        log.info("RequestLoggingFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        if (servletRequest instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) servletRequest;

            // Отримання IP-адреси
            String ipAddress = getClientIpAddress(request);

            // Отримання повного URL запиту
            String requestURL = request.getRequestURL().toString();
            String queryString = request.getQueryString();
            String fullRequestUri = requestURL + (queryString != null ? "?" + queryString : "");

            log.info("Incoming Request: IP={}, URL={}", ipAddress, fullRequestUri);
        }

        // Продовжуємо ланцюжок фільтрів, щоб запит дійшов до контролера
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
        // Логіка очищення (викликається при завершенні роботи фільтра)
        log.info("RequestLoggingFilter destroyed");
    }

    // Допоміжна функція для отримання IP-адреси (як у попередній відповіді)
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}