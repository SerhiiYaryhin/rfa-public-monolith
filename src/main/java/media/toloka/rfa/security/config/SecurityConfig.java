package media.toloka.rfa.security.config;

//https://stackoverflow.com/questions/74753700/cannot-resolve-method-antmatchers-in-authorizationmanagerrequestmatcherregis

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

//
@EnableWebSecurity
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
                        // 🌐 Доступ для всіх без авторизації
                                .anyRequest().permitAll()
//                        .requestMatchers(
//                                "/home", "/register", "/saveUser", "/guest/**", "/process/**", "/seveform/**",
//                                "/post/**", "/rss/**", "/error/**", "/robots.txt",
//                                "/css/**", "/icons/**", "/js/**", "/pictures/**", "/assets/**", // статичні ресурси
//                                "/login/**",  "/login/route", "/logout", "/registerRadioUser", "/restorePsw", "/chat", "/rfachat", // 🔐 Публічні ендпоїнти
//                                "/sendmail", "/setUserPassword", "/savequestion", "/store/**" // 🔐 Публічні ендпоїнти
//                        ).permitAll()

//                         🔐 Публічні ендпоїнти
//                        .requestMatchers(
//                                "/login/**", "/logout", "/registerRadioUser", "/restorePsw", "/chat", "/rfachat",
//                                "/sendmail", "/setUserPassword", "/savequestion"
//                        ).permitAll()

                        // 👮 Доступи за ролями
//                        .requestMatchers("/acc/**","/admin/**").hasAuthority("Admin")
//                        .requestMatchers("/user/**").hasAuthority("User")
//                        .requestMatchers("/creater/**").hasAuthority("Creator")
//                        .requestMatchers("/editor/**").hasAnyAuthority("Editor", "Admin")
//                        .requestMatchers("/moderator/**").hasAnyAuthority("Moderator", "Admin")
//                        .requestMatchers("/upload/**", "/newstoradio/**").hasAnyAuthority("User", "Creator", "Admin", "Editor", "Moderator")
//
//                        // 🔒 Все інше — тільки для авторизованих
//                        .anyRequest().authenticated()
                )

                // 🔐 Форма логіну
                .formLogin(fL -> fL
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/login/route",true)
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

}