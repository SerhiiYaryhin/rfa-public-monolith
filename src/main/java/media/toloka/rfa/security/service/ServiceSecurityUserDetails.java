package media.toloka.rfa.security.service;

import media.toloka.rfa.security.model.Roles;
import media.toloka.rfa.security.model.Users;
import media.toloka.rfa.security.repository.UserSecurityRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ServiceSecurityUserDetails implements UserDetailsService {

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private UserSecurityRepository repoUsers;
    
    @Autowired
    private LoginAttemptService loginAttemptService;
    
    @Autowired
    private jakarta.servlet.http.HttpServletRequest request;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String xf = request.getHeader("X-Forwarded-For");
        String ip = (xf == null) ? request.getRemoteAddr() : xf.split(",")[0];
        
        if (loginAttemptService.isBlocked(ip)) {
            throw new LockedException("IP заблоковано через забагато невдалих спроб входу. Спробуйте через 10 хвилин.");
        }
        
        if (loginAttemptService.isBlocked(email)) {
            throw new LockedException("Акаунт тимчасово заблоковано через забагато невдалих спроб входу. Спробуйте через 10 хвилин.");
        }

        org.springframework.security.core.userdetails.User springUser=null;

        Optional<Users> opt = repoUsers.findUserByEmail(email);
        if(opt.isEmpty()) {
            // Заводимо тимчасового користувача для чата

            throw new UsernameNotFoundException("Користувача з поштою " +email +" не знайдено.");
        }else {
            Users user = opt.get();
            List<Roles> roles = user.getRoles();
            Set<GrantedAuthority> ga = new HashSet<>();
            for(Roles role : roles) {
                String roleName = role.getRole().label;
                if (!roleName.startsWith("ROLE_")) {
                    roleName = "ROLE_" + roleName.toUpperCase();
                }
                ga.add(new SimpleGrantedAuthority(roleName));
            }
            springUser = new org.springframework.security.core.userdetails.User(
                    email,
                    user.getPassword(),
                    ga );
        }
        return springUser;
    }


}
