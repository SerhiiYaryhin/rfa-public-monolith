package media.toloka.rfa.security.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFailureListener implements ApplicationListener<AuthenticationFailureBadCredentialsEvent> {

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Override
    public void onApplicationEvent(AuthenticationFailureBadCredentialsEvent e) {
        String xf = request.getHeader("X-Forwarded-For");
        String ip = (xf == null) ? request.getRemoteAddr() : xf.split(",")[0];
        String username = e.getAuthentication().getName();
        
        loginAttemptService.loginFailed(ip);
        loginAttemptService.loginFailed(username);
    }
}
