package media.toloka.rfa.config;

import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.server.ErrorPageRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

/**
 * Реєструє кастомні сторінки помилок для Spring Boot.
 * Це працює НАД Spring MVC і перехоплює помилки до того,
 * як вони потраплять до Tomcat whitelabel.
 */
@Configuration
public class ErrorPageConfig implements ErrorPageRegistrar {

    @Override
    public void registerErrorPages(ErrorPageRegistry registry) {
        ErrorPage error404 = new ErrorPage(HttpStatus.NOT_FOUND, "/error/404-page");
        ErrorPage error403 = new ErrorPage(HttpStatus.FORBIDDEN, "/error/403-page");
        ErrorPage error500 = new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/error/500-page");
        ErrorPage errorGeneric = new ErrorPage(Throwable.class, "/error/generic-page");

        registry.addErrorPages(error404, error403, error500, errorGeneric);
    }
}
