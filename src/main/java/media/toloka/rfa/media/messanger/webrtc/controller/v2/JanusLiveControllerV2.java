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
 * REST-контролер для керування живими WebRTC трансляціями (v2).
 *
 * Ключові відмінності від v1:
 * - Новий endpoint: /api/v1q/webrtc/stream
 * - Використовує RadioLiveServiceV2
 * - Безпечніше: api_secret НЕ передається клієнту
 */
@RestController
@RequestMapping("/api/v1q/webrtc/stream")
public class JanusLiveControllerV2 {

    private static final Logger logger = LoggerFactory.getLogger(JanusLiveControllerV2.class);

    private final RadioLiveServiceV2 radioLiveService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public JanusLiveControllerV2(RadioLiveServiceV2 radioLiveService, org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate) {
        this.radioLiveService = radioLiveService;
        this.messagingTemplate = messagingTemplate;
    }

    // ============================================================
    // УПРАВЛІННЯ ТРАНЛЯЦІЯМИ
    // ============================================================

    /**
     * Запуск трансляції радіостанції.
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
     * ⚠️ БЕЗПЕКА: api_secret НЕ передається клієнту!
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
                "token", token
        ));
    }

    // ============================================================
    // СПИСОК КІМНАТ
    // ============================================================

    /**
     * Отримання списку активних кімнат з учасниками.
     */
    @GetMapping("/rooms")
    public ResponseEntity<?> getRooms() {
        logger.debug("Запит списку кімнат");

        try {
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
    // МОДЕРАЦІЯ ТА КЕРУВАННЯ УЧАСНИКАМИ
    // ============================================================

    /**
     * Переміщення користувача між кімнатами Janus через WebSocket сигналізацію.
     */
    @PostMapping("/moderate/move")
    public ResponseEntity<?> moveUser(@RequestParam String userUuid, @RequestParam Long targetRoomId) {
        logger.info("Модератор переміщує користувача {} в кімнату {}", userUuid, targetRoomId);
        try {
            messagingTemplate.convertAndSend("/topic/webrtc.control." + userUuid, Map.of(
                    "action", "switch_room",
                    "targetRoomId", targetRoomId
            ));
            return ResponseEntity.ok(Map.of("success", true, "message", "Команду переміщення надіслано"));
        } catch (Exception e) {
            logger.error("Помилка надсилання команди переміщення", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Керування мікрофоном учасника (mute/unmute) в Janus.
     */
    @PostMapping("/moderate/mute")
    public ResponseEntity<?> muteUser(@RequestParam Long roomId, @RequestParam Long participantId, @RequestParam boolean muted) {
        logger.info("Модератор змінює стан мікрофона для учасника {} в кімнаті {}: muted={}", participantId, roomId, muted);
        try {
            radioLiveService.muteParticipant(roomId, participantId, muted);
            return ResponseEntity.ok(Map.of("success", true, "message", muted ? "Мікрофон вимкнено" : "Мікрофон увімкнено"));
        } catch (Exception e) {
            logger.error("Помилка зміни стану мікрофона", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Вигнання учасника з кімнати.
     */
    @PostMapping("/moderate/kick")
    public ResponseEntity<?> kickUser(@RequestParam Long roomId, @RequestParam Long participantId) {
        logger.info("Модератор виганяє учасника {} з кімнати {}", participantId, roomId);
        try {
            radioLiveService.kickParticipant(roomId, participantId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Учасника вигнано з кімнати"));
        } catch (Exception e) {
            logger.error("Помилка вигнання учасника", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Запуск трансляції аудіопотоку з кімнати назовні (rtp_forward).
     */
    @PostMapping("/moderate/forward/start")
    public ResponseEntity<?> startForward(@RequestParam Long roomId, @RequestParam String host, @RequestParam Integer port) {
        logger.info("Модератор запускає RTP forward з кімнати {} на {}:{}", roomId, host, port);
        try {
            Long streamId = radioLiveService.startRtpForward(roomId, host, port);
            return ResponseEntity.ok(Map.of("success", true, "streamId", streamId));
        } catch (Exception e) {
            logger.error("Помилка запуску RTP forward", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Зупинка трансляції аудіопотоку з кімнати назовні.
     */
    @PostMapping("/moderate/forward/stop")
    public ResponseEntity<?> stopForward(@RequestParam Long roomId, @RequestParam Long streamId) {
        logger.info("Модератор зупиняє RTP forward з кімнати {} з streamId {}", roomId, streamId);
        try {
            radioLiveService.stopRtpForward(roomId, streamId);
            return ResponseEntity.ok(Map.of("success", true, "message", "RTP forward зупинено"));
        } catch (Exception e) {
            logger.error("Помилка зупинки RTP forward", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
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
}