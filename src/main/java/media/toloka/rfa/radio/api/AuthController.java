package media.toloka.rfa.radio.api;

import media.toloka.rfa.security.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateAndGetToken(@RequestBody Map<String, String> authRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.get("username"), 
                        authRequest.get("password")
                )
        );
        
        if (authentication.isAuthenticated()) {
            return ResponseEntity.ok(Map.of(
                    "token", jwtService.generateToken(authRequest.get("username"))
            ));
        } else {
            throw new UsernameNotFoundException("Invalid user request!");
        }
    }


    /**
     * Отримати JWT токен для поточного авторизованого користувача (через сесію).
     * Використовується для тестування API з браузера після форм-логіну.
     * GET /api/v1/auth/current-token
     */
    @GetMapping("/current-token")
    public ResponseEntity<?> getCurrentToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "Користувач не авторизований. Увійдіть через форму логіну."
            ));
        }

        // Отримуємо email з UserDetails
        String email;
        if (authentication.getPrincipal() instanceof UserDetails) {
            email = ((UserDetails) authentication.getPrincipal()).getUsername();
        } else {
            email = authentication.getName();
        }

        // Генеруємо JWT токен
        String token = jwtService.generateToken(email);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "token", token,
                "email", email,
                "expiresIn", 36000
        ));
    }
}
