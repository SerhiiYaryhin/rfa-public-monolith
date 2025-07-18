package media.toloka.rfa.config.interceptor;

// https://www.baeldung.com/spring-mvc-handlerinterceptor-vs-filter

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

//public class LoggerInterceptor implements HandlerInterceptor {
//
//    final Logger logger = LoggerFactory.getLogger(LoggerInterceptor.class);
//
////    @Autowired
////    public HttpSession httpSession;
//
//    @Override
//    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
//                             Object handler) {
//        String remoteAddr = request.getHeader("X-FORWARDED-FOR");
//        if (remoteAddr == null || "".equals(remoteAddr)) {
//            remoteAddr = request.getRemoteAddr();
//        }
//        String requestUrl = request.getRequestURL().toString();
//        if (
//                requestUrl.contains("rfa.toloka.media/assets")
//                || requestUrl.contains("rfa.toloka.media/css/")
//                || requestUrl.contains("rfa.toloka.media/js/")
//                || requestUrl.contains("http://localhost:")
//        ) {
//            return true;
//        } else {
//            logger.info(remoteAddr + " => " +request.getRequestURL().toString());
//            String referer = request.getHeader("Referer");
//            if (referer != null) {
//                logger.info("ReferertURL: {}", referer);
//            }
//
//            // Працюємо  із сессією та куками
//            HttpSession httpSession = request.getSession();
//            Cookie[] cookies =  request.getCookies();
//
//
//            if (    cookies != null
//                    &&
//                    cookies.length > 0
//            ) {
//                for (Cookie c : cookies) {
////                    logger.info("Cookies={} value={}", c.getName(), c.getValue());
////                    if (c.getName().equals("LastVisit")) {
////                        Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
////                        logger.info("=== Session ID={} LastVisit={}",httpSession.getId(),formatter.format(Long.parseLong(c.getValue())));
////                    }
//                }
//            }
//            // create a cookie
//            Long ldate = new Date().getTime();
//            Cookie cookie = new Cookie("LastVisit", ldate.toString());
//            //add cookie to response
//            response.addCookie(cookie);
//        }
//        // todo Логировать обращения к серверу.
//        return true;
//    }
//}