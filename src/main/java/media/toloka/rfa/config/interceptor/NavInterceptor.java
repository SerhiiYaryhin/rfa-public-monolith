package media.toloka.rfa.config.interceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import media.toloka.rfa.media.messanger.service.MessangerService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.client.service.ClientService;
//import media.toloka.rfa.radio.message.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
//import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import static java.lang.String.valueOf;
//
//@Component
//public class NavInterceptor implements HandlerInterceptor {
//
//    @Autowired
//    private MessangerService messangerService;
//    @Autowired
//    private ClientService clientService;
//
//    final Logger logger = LoggerFactory.getLogger(NavInterceptor.class);
//
////    @Override
////    public boolean preHandle(
////            HttpServletRequest request,
////            HttpServletResponse response,
////            Object handler) throws Exception {
////
//////        logger.info("[preHandle][" + request + "]" + "[" + request.getMethod()
//////                + "]" + request.getRequestURI());
////
////        return true;
////    }
//    @Override
//    public void postHandle(
//            HttpServletRequest request,
//            HttpServletResponse response,
//            Object handler,
//            ModelAndView modelAndView) throws Exception {
//        if (modelAndView == null) {
//            return;
//        }
//        Clientdetail cd;
//        cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
//        if (cd == null) {
//            return;
//        }
//        // додали кількість повідомлень для меню
//        // Подивитися чому ми викликаємо цю процедуру декілька разів.
//        ModelMap mm = modelAndView.getModelMap();
//        int newmessage = messangerService.GetQuantityNewMessage(cd.getUuid());
//        if ( newmessage > 0 ) {
//            // виставляємо для навбара та повідомлень на сторінці кількість і наявність нових повідомлень
//            modelAndView.getModel().put("quantitynewmessage", newmessage);
//            modelAndView.getModel().put("quantityallmessage", messangerService.GetQuantityAllMessage(cd.getUuid()));
//            modelAndView.getModel().put("danger", "У Вас нові повідомлення: " + valueOf(newmessage));
//        }
//    }
//}
