package media.toloka.rfa.media.messanger.webrtc.controller.v2;

import media.toloka.rfa.media.messanger.webrtc.model.JanusLiveStream;
import media.toloka.rfa.media.messanger.webrtc.service.v2.RadioLiveServiceV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

        return ResponseEntity.ok(Map.of("token", token));
    }

    // ============================================================
    // СПИСОК КІМНАТ
    // ============================================================

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

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        boolean ok = radioLiveService.pingJanus();

        return ResponseEntity.ok(Map.of(
                "janus_admin_api", ok ? "up" : "down",
                "message", ok ? "Зв'язок з Janus встановлено" : "Помилка зв'язку"
        ));
    }

    /**
     * Створення RTP participant для прийому зовнішнього потоку.
     * ✅ ВИПРАВЛЕНО: Прибрано пробіли в ключах Map
     */
    @PostMapping("/create-rtp-participant")
    public ResponseEntity<?> createRtpParticipant(
            @RequestParam Long roomId,
            @RequestParam(defaultValue = "Radio Stream") String display) {

        logger.info("Запит на створення RTP participant: room={}, display={}", roomId, display);

        try {
            Map<String, Object> rtpInfo = radioLiveService.createRtpParticipant(roomId, display);

            return ResponseEntity.ok(Map.of(
                    "status", "created",
                    "participant_id", rtpInfo.get("participantId"),
                    "room_id", roomId,
                    "rtp_ip", rtpInfo.get("rtpIp"),
                    "rtp_port", rtpInfo.get("rtpPort"),
                    "payload_type", rtpInfo.get("payloadType")
            ));
        } catch (Exception e) {
            logger.error("Помилка створення RTP participant", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ));
        }
    }
}