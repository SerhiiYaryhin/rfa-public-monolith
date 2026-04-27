package media.toloka.rfa.config;

import jakarta.servlet.http.HttpServletRequest;
import media.toloka.rfa.radio.station.ClientHomeStationController;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PreprocessRequest {
   // final Logger logger = LoggerFactory.getLogger(PreprocessRequest.class);

//    @Pointcut("within(@org.springframework.web.bind.annotation.RestController*)")
//    public void restcontroller() {}
//
//    @Pointcut("@annotation(org.springframework.web.bind.annotation.PostMapping)")
//    public void postmapping() {}

//    @Pointcut("@annotation(org.springframework.web.bind.annotation.GetMapping)")
//    public void mygetmapping() {}


//    @Pointcut("@annotation(org.springframework.web.bind.annotation.RequestMapping)")
//    public void myRequestMapping() {}
//
////    @Around("restcontroller() && postmapping() && mygetmapping() && args(.., @RequestBody body, request)")
//    @Around("myRequestMapping && args(.., @Model model, @RequestBody body, request)")
//    public Object logPostMethods(ProceedingJoinPoint joinPoint, Object body, HttpServletRequest request) throws Throwable {
//        logger.info(request.toString()); // You may log request parameters here.
//        logger.info(body.toString()); // You may do some reflection here
//
//        Object result;
//        try {
//            result = joinPoint.proceed();
//            logger.info(result.toString());
//            return result;
//        } catch(Throwable t) {}
//        return body;
//    }
}
