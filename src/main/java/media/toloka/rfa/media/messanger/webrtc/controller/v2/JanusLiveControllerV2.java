package media.toloka.rfa.media.messanger.webrtc.controller.v2;

import media.toloka.rfa.media.messanger.webrtc.model.JanusLiveStream;
import media.toloka.rfa.media.messanger.webrtc.service.v2.AudioRoomServiceV2;
import media.toloka.rfa.media.messanger.webrtc.service.v2.BroadcastServiceV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST-контролер для керування живими WebRTC трансляціями (v2).
 * Делегує роботу до двох сервісів:
 * - BroadcastServiceV2 - управління трансляціями
 * - AudioRoomServiceV2 - управління кімнатами та користувачами
 */
@RestController
@RequestMapping("/api/v1q/webrtc/stream")
public class JanusLiveControllerV2 {

    private static final Logger logger = LoggerFactory.getLogger(JanusLiveControllerV2.class);

    private final BroadcastServiceV2 broadcastService;
    private final AudioRoomServiceV2 audioRoomService;

    public JanusLiveControllerV2(
            BroadcastServiceV2 broadcastService,
            AudioRoomServiceV2 audioRoomService) {
        this.broadcastService = broadcastService;
        this.audioRoomService = audioRoomService;
    }

    // ============================================================
    // УПРАВЛІННЯ ТРАНЛЯЦІЯМИ (делегуємо до BroadcastServiceV2)
    // ============================================================

    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestParam String stationId) {
        logger.info("Запит на запуск стріму: stationId={}", stationId);

        try {
            JanusLiveStream stream = broadcastService.startBroadcast(stationId);

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

    @PostMapping("/stop")
    public ResponseEntity<?> stop(@RequestParam String stationId) {
        logger.info("Запит на зупинку стріму: stationId={}", stationId);

        try {
            broadcastService.stopBroadcast(stationId);

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
    // ТОКЕНИ ТА АВТОРИЗАЦІЯ (делегуємо до AudioRoomServiceV2)
    // ============================================================

    @GetMapping("/token")
    public ResponseEntity<?> getToken() {
        logger.debug("Запит токена користувача");

        String token = audioRoomService.generateAndRegisterToken();

        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error", "Unauthorized",
                            "message", "Користувач повинен увійти в систему"
                    ));
        }

        return ResponseEntity.ok(Map.of("token", token));
    }

    // ============================================================
    // СПИСОК КІМНАТ (делегуємо до AudioRoomServiceV2)
    // ============================================================

    @GetMapping("/rooms")
    public ResponseEntity<?> getRooms() {
        logger.debug("Запит списку кімнат");

        try {
            return ResponseEntity.ok(audioRoomService.getActiveRoomsInfo());
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
    // ПІДКЛЮЧЕННЯ ДО КІМНАТИ (делегуємо до AudioRoomServiceV2)
    // ============================================================

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
            Object result = audioRoomService.connectClientToAudioRoom(roomID);
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

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        boolean ok = audioRoomService.pingJanus();

        return ResponseEntity.ok(Map.of(
                "janus_admin_api", ok ? "up" : "down",
                "message", ok ? "Зв'язок з Janus встановлено" : "Помилка зв'язку"
        ));
    }
}