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
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.filter.RequestContextFilter;

@EnableWebSecurity
@EnableMethodSecurity
@Configuration
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthFilter;

    @Autowired
    private UserDetailsService uds;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @Bean
    public FilterRegistrationBean<RequestContextFilter> requestContextFilterRegistration() {
        RequestContextFilter filter = new RequestContextFilter();
        FilterRegistrationBean<RequestContextFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/home", "/register", "/saveUser", "/guest/**",
                                "/post/**", "/podcast/**", "/rss/**", "/error/**", "/robots.txt", "/robot.txt",
                                "/api/1.0/**", "/api/v1/auth/**", "/css/**", "/icons/**", "/js/**", "/pictures/**", "/assets/**",  "/assets/favicon.ico"
                        ).permitAll()
                        
                        .requestMatchers(
                                "/login/**", "/login/route", "/logout", "/registerRadioUser", 
                                "/restorePsw", "/chat", "/rfachat", "/sendmail", 
                                "/setUserPassword", "/savequestion", "/store/**"
                        ).permitAll()

                        .requestMatchers("/acc/**").hasAnyRole("ADMIN", "ACCCHEAF")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/creater/author/**").hasAnyRole("AUTHOR", "ADMIN")
                        .requestMatchers("/api/author/**").hasAnyRole("AUTHOR", "ADMIN")
                        .requestMatchers("/author/apply").authenticated()

                        .requestMatchers("/creater/picprofileupload").authenticated()
                        .requestMatchers("/creater/**").hasAnyRole("CREATER", "MODERATOR", "ADMIN")

                        .requestMatchers("/user/**").hasAnyRole("USER", "CREATER", "MODERATOR", "ADMIN", "NEWSTORADIO")
                        .requestMatchers("/newstoradio/**").hasAnyRole("NEWSTORADIO", "CREATER", "USER", "MODERATOR", "ADMIN")
                        .requestMatchers("/editor/**").hasAnyRole("EDITOR", "ADMIN")
                        .requestMatchers("/moderator/**").hasAnyRole("MODERATOR", "ADMIN")
                        
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(fL -> fL
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/login/route", true)
                        .permitAll()
                )

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
