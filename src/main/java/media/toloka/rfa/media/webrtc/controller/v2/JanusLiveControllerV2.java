package media.toloka.rfa.media.webrtc.controller.v2;

import media.toloka.rfa.media.webrtc.model.JanusLiveStream;
import media.toloka.rfa.media.webrtc.service.v2.RadioLiveServiceV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 📡 REST-контролер для керування живими WebRTC трансляціями та модерацією (версія v2).
 *
 * Ключові особливості версії V2:
 * - Базовий шлях: `/api/v1q/webrtc/stream`
 * - Використовує оптимізований сервіс `RadioLiveServiceV2`.
 * - Підвищена безпека: `api_secret` та `admin_secret` завантажуються лише на бекенді,
 *   браузерний клієнт отримує тільки тимчасові токени (UUID користувача) без доступу до паролів.
 */
@RestController
@RequestMapping("/api/v1q/webrtc/stream")
public class JanusLiveControllerV2 {

    private static final Logger logger = LoggerFactory.getLogger(JanusLiveControllerV2.class);

    // Головний сервіс бізнес-логіки WebRTC трансляцій
    private final RadioLiveServiceV2 radioLiveService;
    
    // Шаблон для відправки повідомлень у STOMP черги (WebSocket сигналізація)
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    /**
     * Конструктор контролера з впровадженням залежностей.
     */
    public JanusLiveControllerV2(RadioLiveServiceV2 radioLiveService, org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate) {
        this.radioLiveService = radioLiveService;
        this.messagingTemplate = messagingTemplate;
    }

    // ============================================================
    // УПРАВЛІННЯ ТРАНЛЯЦІЯМИ (РАДІО)
    // ============================================================

    /**
     * 🎬 Запуск трансляції радіостанції в AudioBridge кімнату.
     * Створює кімнату в Janus, Plain RTP підключення та запускає FFmpeg.
     *
     * @param stationId ID радіостанції (наприклад, "station_802")
     * @return Дані про створений потік (ID кімнати, UDP порт та статус)
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
     * ⏹️ Зупинка трансляції радіостанції.
     * Зупиняє FFmpeg агент та знищує персистентну сесію Plain RTP у Janus.
     *
     * @param stationId ID радіостанції (наприклад, "station_802")
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
     * 🔑 Отримання та реєстрація токена користувача для підключення до Janus.
     *
     * ⚠️ БЕЗПЕКА: Медіасервер Janus вимагає перевірки токенів (Token-based Authentication).
     * Даний метод генерує токен (UUID авторизованого користувача) та реєструє його
     * у Janus через Admin API. Клієнту повертається ТІЛЬКИ токен, без системного api_secret.
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

        // Повертаємо токен користувача
        return ResponseEntity.ok(Map.of(
                "token", token
        ));
    }

    // ============================================================
    // СПИСОК КІМНАТ
    // ============================================================

    /**
     * 🔍 Отримання списку активних кімнат та їхніх учасників .
     * Інформація синхронізується з медіасервером у реальному часі.
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
     * 🚪 Підготовка підключення користувача до конкретної аудіокімнати.
     * Бекенд перевіряє, чи не сидить користувач у інших кімнатах,
     * та примусово виганяє його звідти (евакуація) перед новим входом.
     *
     * @param dataMap Мапа JSON параметрів, що містить "roomid"
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
     * 🔄 Переміщення користувача між кімнатами Janus через WebSocket сигналізацію.
     *
     * @param userUuid Унікальний токен (UUID) користувача, якого переміщуємо
     * @param targetRoomId ID цільової кімнати (наприклад, 900 для "зеленої кімнати")
     */
    @PostMapping("/moderate/move")
    public ResponseEntity<?> moveUser(@RequestParam String userUuid, @RequestParam Long targetRoomId) {
        logger.info("Модератор переміщує користувача {} в кімнату {}", userUuid, targetRoomId);
        try {
            // Відправляємо WebSocket сигнал у персональний канал користувача
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
     * 🔇 Керування мікрофоном учасника (mute/unmute) в Janus.
     *
     * @param roomId ID кімнати
     * @param participantId Числовий ідентифікатор учасника в Janus
     * @param muted true — вимкнути мікрофон, false — увімкнути
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
     * ❌ Вигнання (kick) учасника з кімнати Janus AudioBridge.
     *
     * @param roomId ID кімнати
     * @param participantId Числовий ідентифікатор учасника в Janus
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
     * 📡 Запуск ретрансляції (RTP Forward) аудіопотоку з кімнати Janus на вказані реквізити.
     *
     * @param roomId ID кімнати, потік якої транслюємо назовні
     * @param host IP-адреса отримувача RTP потоку
     * @param port UDP-порт отримувача потоку
     * @return Stream ID унікального потоку ретрансляції (потрібний для зупинки)
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
     * ⏹️ Зупинка ретрансляції (RTP Forward) аудіопотоку.
     *
     * @param roomId ID кімнати
     * @param streamId ID потоку ретрансляції
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
     * 🔍 Перевірка зв'язку з Janus Admin API (Health Check).
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