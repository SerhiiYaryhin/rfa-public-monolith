package media.toloka.rfa.config;

import jakarta.servlet.http.HttpServletRequest;
import media.toloka.rfa.banner.fileupload.BannerDropPostFileController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@ControllerAdvice
public class GlobalModelAttributes {

    final Logger logger = LoggerFactory.getLogger(GlobalModelAttributes.class);


    @ModelAttribute("siteUrl")
    public String getSiteUrl(HttpServletRequest request) {
        // ServletUriComponentsBuilder автоматично перевірить:
        // 1. Заголовки X-Forwarded-Proto (якщо налаштовано стратегію native)
        // 2. Порти та домени, надіслані через проксі

//        String ttt = ServletUriComponentsBuilder.fromCurrentContextPath()
//                .build()
//                .toUriString();
//        logger.info("=========== siteUrl: " + ttt);

        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .build()
                .toUriString();
//        return ttt;
    }
}








//
//import jakarta.servlet.http.HttpServletRequest;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.web.bind.annotation.ControllerAdvice;
//import org.springframework.web.bind.annotation.ModelAttribute;
//
//@ControllerAdvice
//public class GlobalModelAttributes {
//
//    @Value("${media.toloka.rfa.server.globalname:https://rfa.toloka.media}")
//    private String siteUrl;
//
//    @ModelAttribute("siteUrl")
//    public String getSiteUrl(HttpServletRequest request) {
//        // Використовуємо фактичний домен запиту, якщо він відрізняється від конфігурації
//        String requestUrl = request.getRequestURL().toString();
//        String requestDomain = requestUrl.substring(0, requestUrl.indexOf(request.getRequestURI()));
//        return requestDomain;
//    }
//}
