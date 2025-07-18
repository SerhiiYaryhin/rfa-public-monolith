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
//                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 🌐 Доступ для всіх без авторизації (всі публічні шляхи в одному місці)
                        .requestMatchers(
                                "/*","/home", "/register", "/saveUser", "/guest/**", "/process/**", "/seveform/**",
                                "/post/**", "/rss/**", "/error/**", "/robots.txt","/api/**",
                                "/css/**", "/icons/**", "/js/**", "/pictures/**", "/assets/**",
                                "/login/**",  "/login/route", "/logout", "/registerRadioUser", "/restorePsw", "/chat", "/rfachat",
                                "/podcast/**", "/sendmail", "/setUserPassword", "/savequestion", "/store/**",
                                "/user/**","/creater/**","/newstoradio/**","/admin/**","/comments/**","/universalcomments/**"
                        ).permitAll()

                        // 👮 Доступи за ролями (від більш конкретних до загальних)
                        .requestMatchers("/acc/**").hasAuthority("Admin")
                        .requestMatchers("/admin/**").hasAuthority("Admin")
                        .requestMatchers("/user/**").hasAnyAuthority("User", "Moderator", "Admin")
                        .requestMatchers("/creater/**").hasAnyAuthority("Creator", "User", "Moderator", "Admin")
                        .requestMatchers("/newstoradio/**").hasAnyAuthority("Creator", "User", "Moderator", "Admin")
                        .requestMatchers("/editor/**").hasAnyAuthority("Editor", "Admin")
                        .requestMatchers("/moderator/**").hasAnyAuthority("Moderator", "Admin")
                        .requestMatchers("/upload/**").hasAnyAuthority("User", "Creator", "Admin", "Editor", "Moderator")

                        // 🔒 Все інше — тільки для авторизованих (МАЄ БУТИ ОСТАННІМ!)
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

                // 🛡️ Дозволити iframe (наприклад, для H2 console)
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                );

        return http.build();
    }

    // старий

//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//
//        http
//                .authorizeHttpRequests(auth -> auth
//                        // 🌐 Доступ для всіх без авторизації
////                                .anyRequest().permitAll()
//
//                        .requestMatchers(
//                                "/home", "/register", "/saveUser", "/guest/**", "/process/**", "/seveform/**",
//                                "/post/**", "/rss/**", "/error/**", "/robots.txt",
//                                "/css/**", "/icons/**", "/js/**", "/pictures/**", "/assets/**", // статичні ресурси
//                                "/login/**",  "/login/route", "/logout", "/registerRadioUser", "/restorePsw", "/chat", "/rfachat",
//                                "/podcast/**", // 🔐 Публічні ендпоїнти
//                                "/sendmail", "/setUserPassword", "/savequestion", "/store/**", // 🔐 Публічні ендпоїнти
////                        ).permitAll()
////
////                         🔐 Публічні ендпоїнти
////                        .requestMatchers(
//                                "/login/**", "/logout", "/registerRadioUser", "/restorePsw", "/chat", "/rfachat",
//                                "/sendmail", "/setUserPassword", "/savequestion"
//                        ).permitAll()
//
//                        // 👮 Доступи за ролями
//                        .requestMatchers("/acc/**","/admin/**").hasAuthority("Admin")
//                        .requestMatchers("/user/**").hasAnyAuthority("User", "Admin")
//                        .requestMatchers("/creater/**").hasAnyAuthority("Creator", "Admin")
//                        .requestMatchers("/editor/**").hasAnyAuthority("Editor", "Admin")
//                        .requestMatchers("/moderator/**").hasAnyAuthority("Moderator", "Admin")
//                        .requestMatchers("/upload/**", "/newstoradio/**").hasAnyAuthority("User", "Creator", "Admin", "Editor", "Moderator")
//
//                        // 🔒 Все інше — тільки для авторизованих
//                        .anyRequest().authenticated()
//                                .anyRequest().permitAll()
//                )
//
//                // 🔐 Форма логіну
//                .formLogin(fL -> fL
//                        .loginPage("/login")
//                        .loginProcessingUrl("/login")
//                        .defaultSuccessUrl("/login/route",true)
//                        .permitAll()
//                )
//
//                // 🚪 Логаут
//                .logout(logout -> logout
//                        .invalidateHttpSession(true)
//                        .clearAuthentication(true)
//                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
//                        .logoutSuccessUrl("/")
//                        .permitAll()
//                )
//
//                // 🛡️ Дозволити iframe (наприклад, для H2 console)
//                .headers(headers -> headers
//                        .frameOptions(frame -> frame.sameOrigin())
//                );
//
//        return http.build();
//    }


    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder.userDetailsService(uds).passwordEncoder(encoder);
        return authBuilder.build();
        //        return http.getSharedObject(AuthenticationManagerBuilder.class)
//                .userDetailsService(uds)
//                .passwordEncoder(encoder)
//                .and()
//                .build();
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

}