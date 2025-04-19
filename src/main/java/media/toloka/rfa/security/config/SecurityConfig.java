package media.toloka.rfa.security.config;

//https://stackoverflow.com/questions/74753700/cannot-resolve-method-antmatchers-in-authorizationmanagerrequestmatcherregis

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                        .requestMatchers(
                                "/home", "/register", "/saveUser", "/guest/**", "/process/**", "/seveform/**",
                                "/post/**", "/rss/**", "/error/**", "/robots.txt"
                        ).permitAll()

                        // 📦 Статичні ресурси
                        .requestMatchers(
                                "/css/**", "/icons/**", "/js/**", "/pictures/**", "/assets/**"
                        ).permitAll()

                        // 🔐 Публічні ендпоїнти
                        .requestMatchers(
                                "/login/**", "/logout", "/registerRadioUser", "/restorePsw", "/chat", "/rfachat",
                                "/sendmail", "/setUserPassword", "/savequestion"
                        ).permitAll()

                        // 👮 Доступи за ролями
                        .requestMatchers("/acc/**","/admin/**").hasAuthority("Admin")
                        .requestMatchers("/user/**").hasAuthority("User")
                        .requestMatchers("/creater/**").hasAuthority("Creator")
                        .requestMatchers("/editor/**").hasAnyAuthority("Editor", "Admin")
                        .requestMatchers("/moderator/**").hasAnyAuthority("Moderator", "Admin")
                        .requestMatchers("/upload/**", "/newstoradio/**").hasAnyAuthority("User", "Creator", "Admin", "Editor", "Moderator")

                        // 🔒 Все інше — тільки для авторизованих
                        .anyRequest().authenticated()
                )

                // 🔐 Форма логіну
                .formLogin(fL -> fL
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/login/route")
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

//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//
//        http.authorizeRequests()
//                .requestMatchers(
//                        "/home",
//                        "/register",
//                        "/saveUser",
//                        "/guest/**",
//                        "/process/**",
//                        "/seveform/**",
//                        "/post/**",
//                        "/rss/**",
//                        "/acc/**",
//                        "/**",
//                        "/error/**"
//                ).permitAll()
//                .requestMatchers(
//                        "/css/**",
//                        "/icons/**",
//                        "/js/**",
//                        "/pictures/**"
//                ).permitAll()
//                .requestMatchers(
//                        "/assets/**",
//                        "/savequestion"
//                ).permitAll()
//                .requestMatchers("/login/**",
//                        "/logout",
//                        "/registerRadioUser",
//                        "/restorePsw",
//                        "/chat",
//                        "/rfachat",
////                        "/rfachat/**",
//                        "/sendmail",
//                        "/setUserPassword").permitAll()
//                .requestMatchers("/robots.txt").permitAll()
//                .requestMatchers("/admin/**").hasAuthority("Admin")
//                .requestMatchers("/user/**").hasAuthority("User")
//                .requestMatchers("/creater/**").hasAuthority("Creator")
//                .requestMatchers("/editor/**").hasAuthority("Editor,Admin")
//                .requestMatchers("/moderator/**").hasAuthority("Moderator,Admin")
//                .requestMatchers("/upload/**").hasAuthority("User,Creator,Admin,Editor,Moderator")
//                .requestMatchers("/newstoradio/**").hasAuthority("User,Creator,Admin,Editor,Moderator")
////                .requestMatchers("/upload/music/**").hasAnyAuthority("Editor,User,Admin,Creator")
//                .anyRequest().authenticated();
//                //.anyRequest().permitAll();
//// tmp comment
//        http.formLogin(fL -> fL
//                .loginPage("/login")
//                .loginProcessingUrl("/login")
//                .defaultSuccessUrl("/login/route")
//                .permitAll()
//        );
//
//        http.logout(lOut -> {
//            lOut.invalidateHttpSession(true)
//                    .clearAuthentication(true)
//                    .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
//                    .logoutSuccessUrl("/")
//                    .permitAll();
//        });
//
//        http.headers().frameOptions().sameOrigin();
//
//        return http.build();
//
//    }

//    @Override
//    protected void configure(HttpSecurity http) throws Exception {
//        http.headers().frameOptions().sameOrigin();
//    }
//
////    @Bean
////    public AuthenticationProvider authenticationProvider() {
////        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
////        authenticationProvider.setUserDetailsService(uds);
////        authenticationProvider.setPasswordEncoder(encoder);
////        return authenticationProvider;
////    }
}