package media.toloka.rfa.security.config;

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
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**")) // Ігноруємо CSRF для API, якщо це потрібно для Editor.js
                .authorizeHttpRequests(auth -> auth
                        // 🌐 Публічні ресурси
                        .requestMatchers(
                                "/", "/home", "/register", "/saveUser", "/guest/**",
                                "/post/**", "/rss/**", "/error/**", "/robots.txt", "/api/2.0/**",
                                "/css/**", "/icons/**", "/js/**", "/pictures/**", "/assets/**"
                        ).permitAll()
                        
                        // 🔐 Публічні ендпоїнти логіну та реєстрації
                        .requestMatchers(
                                "/login/**", "/login/route", "/logout", "/registerRadioUser", 
                                "/restorePsw", "/chat", "/rfachat", "/sendmail", 
                                "/setUserPassword", "/savequestion", "/store/**"
                        ).permitAll()

                        // 👮 Доступи за ролями
                        .requestMatchers("/acc/**").hasAnyRole("ADMIN", "ACCCHEAF")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        
                        // Авторська колонка
                        .requestMatchers("/creater/author/**").hasAnyRole("AUTHOR", "ADMIN")
                        .requestMatchers("/api/author/**").hasAnyRole("AUTHOR", "ADMIN")
                        .requestMatchers("/author/apply").authenticated() // Кожен авторизований може подати заявку
                        
                        .requestMatchers("/creater/**").hasAnyRole("CREATER", "MODERATOR", "ADMIN")
                        .requestMatchers("/user/**").hasAnyRole("USER", "CREATER", "MODERATOR", "ADMIN")
                        .requestMatchers("/newstoradio/**").hasAnyRole("CREATER", "USER", "MODERATOR", "ADMIN")
                        .requestMatchers("/editor/**").hasAnyRole("EDITOR", "ADMIN")
                        .requestMatchers("/moderator/**").hasAnyRole("MODERATOR", "ADMIN")
                        
                        // 🔒 Все інше
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

                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
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
        filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return filterRegistrationBean;
    }

    @Bean
    public FilterRegistrationBean<CharacterEncodingFilter> characterEncodingFilterRegistrationBean() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);
        FilterRegistrationBean<CharacterEncodingFilter> registrationBean = new FilterRegistrationBean<>(filter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registrationBean;
    }
}
