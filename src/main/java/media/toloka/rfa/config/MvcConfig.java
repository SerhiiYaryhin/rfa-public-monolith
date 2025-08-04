package media.toloka.rfa.config;

import media.toloka.rfa.comments.config.StringToECommentSourceTypeConverter;
//import media.toloka.rfa.config.interceptor.LoggerInterceptor;
//import media.toloka.rfa.config.interceptor.NavInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
//import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurity6;

// перехоплення вхідного запиту до обробки
// https://stackoverflow.com/questions/58980041/how-can-i-get-the-client-ip-address-of-requests-in-spring-boot
// використати для збору інформації про абсолютно всіх відвідувачів.


@Configuration
public class MvcConfig implements WebMvcConfigurer, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Autowired
    private StringToECommentSourceTypeConverter stringToECommentSourceTypeConverter;

//    @Autowired
//    NavInterceptor navInterceptor;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(stringToECommentSourceTypeConverter);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

//        registry.addInterceptor(navInterceptor);
//        registry.addInterceptor(new LoggerInterceptor());

    }

    public MvcConfig() {
        super();
    }


    public void setApplicationContext(final ApplicationContext applicationContext)
            throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/");

        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");

        registry.addResourceHandler("/icons/**")
                .addResourceLocations("classpath:/static/icons/");

        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");

        registry.addResourceHandler("/pictures/**")
                .addResourceLocations("classpath:/static/pictures/");

        registry.addResourceHandler("/upload/**")
                .addResourceLocations("classpath:/upload/");

        registry.addResourceHandler("/newstoradio/**")
                .addResourceLocations("classpath:/newstoradio/");

        registry.addResourceHandler("/podcast/**")
                .addResourceLocations("classpath:/podcast/");
        registry.addResourceHandler("/error/**")
                .addResourceLocations("classpath:/error/");

        registry.addResourceHandler("/robots.txt")
                .addResourceLocations("classpath:/static/");

//                .addResourceLocations("/static/robots.txt");
//                .addResourceLocations("/static/robots.txt");

        registry.addResourceHandler("/acc/**");

    }



    /* **************************************************************** */
    /*  THYMELEAF-SPECIFIC ARTIFACTS                                    */
    /*  TemplateResolver <- TemplateEngine <- ViewResolver              */
    /* **************************************************************** */

    @Bean
    public SpringResourceTemplateResolver templateResolver(){
        // SpringResourceTemplateResolver automatically integrates with Spring's own
        // resource resolution infrastructure, which is highly recommended.
        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
        templateResolver.setApplicationContext(this.applicationContext);
        templateResolver.setPrefix("classpath:/templates");
        templateResolver.setSuffix(".html");
//        // HTML is the default value, added here for the sake of clarity.
        templateResolver.setTemplateMode(TemplateMode.HTML);
//        // Template cache is true by default. Set to false if you want
//        // templates to be automatically updated when modified.

        templateResolver.setCacheable(true);
        // --- Додайте цей рядок ---
//        templateResolver.setCharacterEncoding("UTF-8");
        return templateResolver;
    }

    @Bean
    public SpringTemplateEngine templateEngine(){
        // SpringTemplateEngine automatically applies SpringStandardDialect and
        // enables Spring's own MessageSource message resolution mechanisms.
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver());
        templateEngine.addDialect(new SpringSecurityDialect());
        // Enabling the SpringEL compiler with Spring 4.2.4 or newer can
        // speed up execution in most scenarios, but might be incompatible
        // with specific cases when expressions in one template are reused
        // across different data types, so this flag is "false" by default
        // for safer backwards compatibility.
        templateEngine.setEnableSpringELCompiler(true);
        return templateEngine;
    }

    @Bean
    public ThymeleafViewResolver viewResolver(){
        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine());
        return viewResolver;
    }

//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        registry.addInterceptor(new LoggerInterceptor());
//    }
//    public void addViewControllers(ViewControllerRegistry registry) {
//        registry.addViewController("/home").setViewName("home");
//        registry.addViewController("/").setViewName("home");
//        registry.addViewController("/hello").setViewName("hello");
//        registry.addViewController("/login").setViewName("login");
//        registry.addViewController("/logout").setViewName("logout");
//    }
}
