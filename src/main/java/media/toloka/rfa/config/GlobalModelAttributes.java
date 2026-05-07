package media.toloka.rfa.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    @Value("${media.toloka.rfa.server.globalname:https://rfa.toloka.media}")
    private String siteUrl;

    @ModelAttribute("siteUrl")
    public String getSiteUrl(HttpServletRequest request) {
        // Використовуємо фактичний домен запиту, якщо він відрізняється від конфігурації
        String requestUrl = request.getRequestURL().toString();
        String requestDomain = requestUrl.substring(0, requestUrl.indexOf(request.getRequestURI()));
        return requestDomain;
    }
}
