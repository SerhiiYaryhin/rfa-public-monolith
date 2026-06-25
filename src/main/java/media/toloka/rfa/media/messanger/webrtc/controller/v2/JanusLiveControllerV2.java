package media.toloka.rfa.media.messanger.webrtc.controller.v2;

import media.toloka.rfa.media.messanger.webrtc.model.JanusLiveStream;
import media.toloka.rfa.media.messanger.webrtc.service.v2.RadioLiveServiceV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Оптимізований REST-контролер для керування WebRTC трансляціями.
 *
 * Ключові відмінності від v1:
 * - Новий endpoint: /api/v1q/webrtc/stream
 * - Використовує RadioLiveServiceV2 з оптимізованими запитами
 * - Прибрано зайві перевірки токенів у GET-запитах
 * - Безпечніше: api_secret НЕ передається клієнту
 */
@RestController
@RequestMapping("/api/v1q/webrtc/stream")
public class JanusLiveControllerV2 {

    private static final Logger logger = LoggerFactory.getLogger(JanusLiveControllerV2.class);

    private final RadioLiveServiceV2 radioLiveService;

    public JanusLiveControllerV2(RadioLiveServiceV2 radioLiveService) {
        this.radioLiveService = radioLiveService;
    }

    // ============================================================
    // УПРАВЛІННЯ ТРАНЛЯЦІЯМИ
    // ============================================================

    /**
     * Запуск трансляції радіостанції.
     *
     * @param stationId ID станції (наприклад, "rfa-rock")
     * @return Дані про створену кімнату
     */
    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestParam String stationId) {
        logger.info("Запит на запуск стріму: stationId={}", stationId);

        try {
            JanusLiveStream stream = radioLiveService.startBroadcast(stationId);

            return ResponseEntity.ok(Map.of(
                    "status", "started",
                    "room_id", stream.getJanusRoomId(),
                    "rtp_port", stream.getRtpPort(),
                    "station_id", stationId
            ));

        } catch (Exception e) {
            logger.error("Помилка запуску стріму", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", "Помилка запуску: " + e.getMessage()
                    ));
        }
    }

    /**
     * Зупинка трансляції.
     *
     * @param stationId ID станції
     */
    @PostMapping("/stop")
    public ResponseEntity<?> stop(@RequestParam String stationId) {
        logger.info("Запит на зупинку стріму: stationId={}", stationId);

        try {
            radioLiveService.stopBroadcast(stationId);

            return ResponseEntity.ok(Map.of(
                    "status", "stopped",
                    "station_id", stationId
            ));

        } catch (Exception e) {
            logger.error("Помилка зупинки стріму", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", "Помилка зупинки: " + e.getMessage()
                    ));
        }
    }

    // ============================================================
    // ТОКЕНИ ТА АВТОРИЗАЦІЯ
    // ============================================================

    /**
     * Отримання токена користувача.
     *
     * ⚠️ БЕЗПЕКА: api_secret НЕ передається клієнту!
     *
     * @return Токен користувача (UUID)
     */
    @GetMapping("/token")
    public ResponseEntity<?> getToken() {
        logger.debug("Запит токена користувача");

        String token = radioLiveService.generateAndRegisterToken();

        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error", "Unauthorized",
                            "message", "Користувач повинен увійти в систему"
                    ));
        }

        // ✅ БЕЗПЕЧНО: тільки токен, без api_secret
        return ResponseEntity.ok(Map.of(
                "token", token,
                "display", getCurrentUserName() // опціонально
        ));
    }

    // ============================================================
    // СПИСОК КІМНАТ (ОПТИМІЗОВАНО)
    // ============================================================

    /**
     * Отримання списку активних кімнат з учасниками.
     *
     * ОПТИМІЗАЦІЯ v2:
     * - Прибрано checkUserTokenInJanus() з цього методу
     * - Перевірка токена відбувається тільки при /token
     * - Менше запитів до Janus = швидша відповідь
     */
    @GetMapping("/rooms")
    public ResponseEntity<?> getRooms() {
        logger.debug("Запит списку кімнат");

        try {
            // ✅ ОПТИМІЗОВАНО: без зайвої перевірки токена
            return ResponseEntity.ok(radioLiveService.getActiveRoomsInfo());

        } catch (Exception e) {
            logger.error("Помилка отримання списку кімнат", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal Server Error",
                            "message", "Помилка отримання даних: " + e.getMessage()
                    ));
        }
    }

    // ============================================================
    // ПІДКЛЮЧЕННЯ ДО КІМНАТИ
    // ============================================================

    /**
     * Підключення користувача до аудіокімнати.
     *
     * @param dataMap JSON з параметром "roomid"
     * @return Дані для підключення до кімнати
     */
    @PostMapping("/connectclienttoroom")
    public ResponseEntity<?> connectClientToRoom(@RequestBody Map<String, Object> dataMap) {
        logger.info("Запит на підключення до кімнати");

        if (dataMap == null || !dataMap.containsKey("roomid")) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", "Відсутній параметр roomid"
                    ));
        }

        Number roomIdNum = (Number) dataMap.get("roomid");
        if (roomIdNum == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "error", "Параметр roomid не може бути порожнім"
                    ));
        }

        Long roomID = roomIdNum.longValue();

        try {
            Object result = radioLiveService.connectClientToAudioRoom(roomID);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Помилка підключення до кімнати {}", roomID, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Помилка підключення: " + e.getMessage()
                    ));
        }
    }

    // ============================================================
    // ДІАГНОСТИКА
    // ============================================================

    /**
     * Перевірка зв'язку з Janus Admin API.
     */
    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        boolean ok = radioLiveService.pingJanus();

        return ResponseEntity.ok(Map.of(
                "janus_admin_api", ok ? "up" : "down",
                "message", ok ? "Зв'язок з Janus встановлено" : "Помилка зв'язку"
        ));
    }

    // ============================================================
    // ДОПОМІЖНІ МЕТОДИ
    // ============================================================

    /**
     * Отримує ім'я поточного користувача (опціонально для UI).
     */
    private String getCurrentUserName() {
        try {
            // Тут має бути ваша логіка отримання імені користувача
            // Наприклад: clientService.GetCurrentUser().getName()
            return "User"; // TODO: реалізувати
        } catch (Exception e) {
            return "Anonymous";
        }
    }
}