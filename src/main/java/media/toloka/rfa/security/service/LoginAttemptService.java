package media.toloka.rfa.security.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Map;

@Service
public class LoginAttemptService {

    private final int MAX_ATTEMPT = 10;
    private final long BLOCK_DURATION = TimeUnit.MINUTES.toMillis(10);
    
    // Ключ: IP або username, Значення: об'єкт спроби
    private Map<String, Attempt> attemptsCache = new ConcurrentHashMap<>();

    public void loginSucceeded(String key) {
        attemptsCache.remove(key);
    }

    public void loginFailed(String key) {
        Attempt attempt = attemptsCache.get(key);
        if (attempt == null) {
            attempt = new Attempt();
        }
        attempt.count++;
        attempt.lastAttempt = System.currentTimeMillis();
        attemptsCache.put(key, attempt);
    }

    public boolean isBlocked(String key) {
        Attempt attempt = attemptsCache.get(key);
        if (attempt == null) {
            return false;
        }
        
        if (attempt.count >= MAX_ATTEMPT) {
            long timePassed = System.currentTimeMillis() - attempt.lastAttempt;
            if (timePassed < BLOCK_DURATION) {
                return true;
            } else {
                // Час блокування минув, скидаємо лічильник
                attemptsCache.remove(key);
                return false;
            }
        }
        return false;
    }
    
    public long getRemainingBlockTime(String key) {
        Attempt attempt = attemptsCache.get(key);
        if (attempt == null) return 0;
        long remaining = BLOCK_DURATION - (System.currentTimeMillis() - attempt.lastAttempt);
        return Math.max(0, remaining);
    }

    private static class Attempt {
        int count = 0;
        long lastAttempt = 0;
    }
}
