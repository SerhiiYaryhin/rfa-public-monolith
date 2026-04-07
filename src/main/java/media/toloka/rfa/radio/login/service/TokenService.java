package media.toloka.rfa.radio.login.service;

import media.toloka.rfa.radio.model.Token;
import media.toloka.rfa.radio.repository.TokenRepository;
import media.toloka.rfa.security.model.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class TokenService {

    @Autowired
    private TokenRepository tokenRepository;

    public void createVerificationToken (Users user, String token) {
        Token stoken = new Token();
        stoken.setUser(user);
        stoken.setToken(token);
//        stoken.setExpiryDate();
        tokenRepository.save(stoken);
    }

    public Token findByToken(String stoken) {
        Optional<Token> token = tokenRepository.findByToken(stoken);
        if (token.isEmpty()) {
            return null;
        } else {
            return token.get();
        }
    }
   public Token findByUser(Users user) {
       Optional<Token> token = tokenRepository.findByUser(user);
       if (token.isEmpty()) {
           return null;
       } else {
           return token.get();
       }
   }
    public void delete(Token myToken) {
        tokenRepository.delete(myToken);
    }
}
