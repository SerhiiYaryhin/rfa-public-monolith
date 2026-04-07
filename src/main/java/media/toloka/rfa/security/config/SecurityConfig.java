package media.toloka.rfa.security.config;

//https://stackoverflow.com/questions/74753700/cannot-resolve-method-antmatchers-in-authorizationmanagerrequestmatcherregis

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.HiddenHttpMethodFilter;

//
@EnableWebSecurity
@EnableMethodSecurity
@Configuration
public class SecurityConfig {

    @Autowired
    private UserDetailsService uds;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 🌐 ПУБЛІЧНІ шляхи — доступ БЕЗ авторизації
                        // ТІЛЬКО те, що дійсно має бути доступне всім (гості, анонімні користувачі)
                        .requestMatchers(
                                "/", "/home",
                                "/register", "/saveUser", "/registerRadioUser",
                                "/guest/**",
                                "/post/**",
                                "/rss/**",
                                "/podcast/**",
                                "/store/**",
                                "/comments/**", "/universalcomments/**",
                                "/error/**", "/robots.txt",
                                "/api/**",
                                "/css/**", "/icons/**", "/js/**", "/pictures/**", "/assets/**",
                                "/login/**", "/login/route",
                                "/logout",
                                "/restorePsw", "/setUserPassword",
                                "/sendmail", "/savequestion",
                                "/process/**", "/seveform/**",
                                "/chat", "/rfachat"
                        ).permitAll()

                        // 🔐 ЗАХИЩЕНІ шляхи — доступ ТІЛЬКИ для авторизованих з відповідними ролями
                        .requestMatchers("/acc/**").hasAuthority("Admin")
                        .requestMatchers("/admin/**").hasAuthority("Admin")
                        .requestMatchers("/moderator/**").hasAuthority("Moderator")
                        .requestMatchers("/editor/**").hasAuthority("Editor")
                        .requestMatchers("/creater/**").hasAnyAuthority("Creator", "User", "Moderator", "Admin")
                        .requestMatchers("/newstoradio/**").hasAnyAuthority("Creator", "User", "Moderator", "Admin")
                        .requestMatchers("/user/**").hasAnyAuthority("User", "Creator", "Moderator", "Admin")
                        .requestMatchers("/upload/**").hasAnyAuthority("User", "Creator", "Admin", "Editor", "Moderator")

                        // 🔒 Все інше — тільки для авторизованих
                        .anyRequest().authenticated()
                )

                // 🔐 Форма логіну
                .formLogin(fL -> fL
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/login/route", true)
                        .permitAll()
                )

                // 🚪 Логаут
                .logout(logout -> logout
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/")
                        .permitAll()
                )

                // 🛡️ Security Headers
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .contentTypeOptions(contentType -> {})  // X-Content-Type-Options: nosniff
                        .xssProtection(xss -> xss.headerValue(
                                org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK
                        ))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .maxAgeInSeconds(31536000)  // 1 рік
                                .includeSubDomains(true)
                                .preload(true)
                        )
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                "script-src 'self' 'unsafe-inline' https://use.fontawesome.com https://cdn.jsdelivr.net https://fonts.googleapis.com; " +
                                "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com; " +
                                "img-src 'self' data: https:; " +
                                "font-src 'self' https://fonts.gstatic.com https://cdn.jsdelivr.net; " +
                                "connect-src 'self'; " +
                                "frame-ancestors 'self'; " +
                                "base-uri 'self'; " +
                                "form-action 'self'"
                        ))
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder.userDetailsService(uds).passwordEncoder(encoder);
        return authBuilder.build();
    }

    @Bean
    public FilterRegistrationBean<HiddenHttpMethodFilter> hiddenHttpMethodFilter() {
        FilterRegistrationBean<HiddenHttpMethodFilter> filterRegistrationBean = new FilterRegistrationBean<>(new HiddenHttpMethodFilter());
        // Встановлюємо порядок фільтра, щоб він виконувався дуже рано,
        // перед більшістю фільтрів Spring Security.
        // Ordered.HIGHEST_PRECEDENCE забезпечує виконання фільтра на максимально ранньому етапі.
        filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return filterRegistrationBean;
    }

    // --- Додайте цей Bean для CharacterEncodingFilter ---
    @Bean
    public FilterRegistrationBean<CharacterEncodingFilter> characterEncodingFilterRegistrationBean() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true); // Завжди примусово встановлювати UTF-8

        FilterRegistrationBean<CharacterEncodingFilter> registrationBean = new FilterRegistrationBean<>(filter);
        registrationBean.addUrlPatterns("/*"); // Застосовувати до всіх URL
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1); // Встановлюємо порядок
        return registrationBean;
    }

}