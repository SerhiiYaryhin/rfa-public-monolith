package media.toloka.rfa.security.service;

import media.toloka.rfa.security.model.Roles;
import media.toloka.rfa.security.model.Users;
import media.toloka.rfa.security.repository.UserSecurityRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
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
//public class ServiceSecurityUserDetails implements ServiceSecurityUsers, UserDetailsService {

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private UserSecurityRepository repoUsers;
//    @Override
    public Long saveUser(@NotNull Users user) { // TODO Видалити. Без використання
        String passwd= user.getPassword();
        String encodedPasswod = passwordEncoder.encode(passwd);
        user.setPassword(encodedPasswod);
        user = repoUsers.save(user);
        return user.getId();
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        org.springframework.security.core.userdetails.User springUser=null;

        Optional<Users> opt = repoUsers.findUserByEmail(email);
        if(opt.isEmpty()) {
            // Заводимо тимчасового користувача для чата

            throw new UsernameNotFoundException("Користувача з поштою " +email +" не знайдено.");
        }else {
            Users user = opt.get();
            List<Roles> roles = user.getRoles();
            Set<GrantedAuthority> ga = new HashSet<>();
            for(Roles role:roles) {
                ga.add(new SimpleGrantedAuthority(role.getRole().label));
            }
            springUser = new org.springframework.security.core.userdetails.User(
                    email,
                    user.getPassword(),
                    ga );
        }
        return springUser;
    }


}
