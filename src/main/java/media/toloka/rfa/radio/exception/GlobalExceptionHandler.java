package media.toloka.rfa.radio.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllExceptions(Exception ex, WebRequest request) {
        String path = request.getDescription(false);
        
        // Повертаємо JSON тільки для API
        if (path.contains("/api/")) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("timestamp", LocalDateTime.now().toString());
            body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            body.put("error", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
            body.put("message", ex.getMessage());
            body.put("path", path.replace("uri=", ""));

            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Для веб-сторінок повертаємо null, щоб Spring Boot сам обробив помилку через ErrorController
        return null;
    }
}
