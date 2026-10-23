package media.toloka.rfa.media.webrtc.controller.v2;

import media.toloka.rfa.media.messanger.model.MessageRoom;
import media.toloka.rfa.media.messanger.repository.MessageRoomRepository;
import media.toloka.rfa.media.webrtc.model.JanusLiveStream;
import media.toloka.rfa.media.webrtc.service.v2.RadioLiveServiceV2;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.radio.repository.StationRepo;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
 * - 🆕 УСІ модераційні дії та керування трансляцією захищені перевіркою:
 *   користувач авторизований І є власником станції, до якої належить кімната.
 */
@RestController
@RequestMapping("/api/v1q/webrtc/stream")
public class JanusLiveControllerV2 {

    private static final Logger logger = LoggerFactory.getLogger(JanusLiveControllerV2.class);

    private final RadioLiveServiceV2 radioLiveService;
    private final SimpMessagingTemplate messagingTemplate;
    // 🆕 Залежності для перевірки прав
    private final ClientService clientService;
    private final StationRepo stationRepo;
    private final MessageRoomRepository messageRoomRepository;

    public JanusLiveControllerV2(RadioLiveServiceV2 radioLiveService,
                                 SimpMessagingTemplate messagingTemplate,
                                 ClientService clientService,
                                 StationRepo stationRepo,
                                 MessageRoomRepository messageRoomRepository) {
        this.radioLiveService = radioLiveService;
        this.messagingTemplate = messagingTemplate;
        this.clientService = clientService;
        this.stationRepo = stationRepo;
        this.messageRoomRepository = messageRoomRepository;
    }

    // ============================================================
    // ЗАХИСТ (HELPERS)
    // ============================================================

    /**
     * 🆕 Пошук станції за числовим ID кімнати Janus (= MessageRoom.id).
     * Перевіряє всі чотири можливі прив'язки: main/green/broadcast/staff.
     */
    private Station findStationByJanusRoomId(Long roomId) {
        if (roomId == null) return null;
        MessageRoom room = messageRoomRepository.findById(roomId).orElse(null);
        if (room == null) return null;
        String uuid = room.getUuid();
        for (Station s : stationRepo.findAll()) {
            if (uuid.equals(s.getRoomuuid())
                    || uuid.equals(s.getGreenRoomUuid())
                    || uuid.equals(s.getBroadcastRoomUuid())
                    || uuid.equals(s.getStaffRoomUuid())) {
                return s;
            }
        }
        return null;
    }

    /**
     * 🆕 Резолв станції з рядкового stationId (UUID або формат "station_802", або числовий id).
     */
    private Station resolveStation(String stationId) {
        if (stationId == null || stationId.isBlank()) return null;
        Station s = stationRepo.getStationByUuid(stationId);
        if (s != null) return s;
        String raw = stationId.startsWith("station_") ? stationId.substring("station_".length()) : stationId;
        try {
            return stationRepo.findById(Long.parseLong(raw)).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 🆕 Універсальний guard: повертає ResponseEntity з помилкою, або null, якщо доступ дозволено.
     * Перевірка: авторизація → існування станції → власник станції.
     */
    private ResponseEntity<?> guard(Station station) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "Необхідна авторизація"));
        }
        if (station == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", "Станцію не знайдено"));
        }
        // TODO: розширити до ролей модераторів (окремий етап RBAC). Зараз — тільки власник.
        if (station.getClientdetail() == null
                || station.getClientdetail().getUser() == null
                || !station.getClientdetail().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "error", "Недостатньо прав для керування цією станцією"));
        }
        return null;
    }

    /** Guard для дій, прив'язаних до кімнати (roomId = Janus room id = MessageRoom.id). */
    private ResponseEntity<?> guardRoom(Long roomId) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "Необхідна авторизація"));
        }
        return guard(findStationByJanusRoomId(roomId));
    }

    // ============================================================
    // УПРАВЛІННЯ ТРАНСЛЯЦІЯМИ (РАДІО)
    // ============================================================

    /**
     * 🎬 Запуск трансляції радіостанції в AudioBridge кімнату.
     * 🆕 Захищено: тільки власник станції.
     */
    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestParam String stationId) {
        logger.info("Запит на запуск стріму: stationId={}", stationId);
        ResponseEntity<?> denied = guard(resolveStation(stationId));
        if (denied != null) return denied;
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
                    .body(Map.of("status", "error", "message", "Помилка запуску: " + e.getMessage()));
        }
    }

    /**
     * ⏹️ Зупинка трансляції радіостанції.
     * 🆕 Захищено: тільки власник станції.
     */
    @PostMapping("/stop")
    public ResponseEntity<?> stop(@RequestParam String stationId) {
        logger.info("Запит на зупинку стріму: stationId={}", stationId);
        ResponseEntity<?> denied = guard(resolveStation(stationId));
        if (denied != null) return denied;
        try {
            radioLiveService.stopBroadcast(stationId);
            return ResponseEntity.ok(Map.of("status", "stopped", "station_id", stationId));
        } catch (Exception e) {
            logger.error("Помилка зупинки стріму", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Помилка зупинки: " + e.getMessage()));
        }
    }

    // ============================================================
    // ТОКЕНИ ТА АВТОРИЗАЦІЯ
    // ============================================================

    /**
     * 🔑 Отримання та реєстрація токена користувача для підключення до Janus.
     * (Без змін: повертає 401, якщо користувач не авторизований.)
     */
    @GetMapping("/token")
    public ResponseEntity<?> getToken() {
        logger.debug("Запит токена користувача");
        String token = radioLiveService.generateAndRegisterToken();
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Користувач повинен увійти в систему"));
        }
        return ResponseEntity.ok(Map.of("token", token));
    }

    // ============================================================
    // СПИСОК КІМНАТ
    // ============================================================

    /**
     * 🔍 Отримання списку активних кімнат та їхніх учасників.
     * (Діагностичний ендпоінт; для панелі використовуйте /api/v1/broadcast/{stationUuid}/status)
     */
    @GetMapping("/rooms")
    public ResponseEntity<?> getRooms() {
        logger.debug("Запит списку кімнат");
        try {
            return ResponseEntity.ok(radioLiveService.getActiveRoomsInfo());
        } catch (Exception e) {
            logger.error("Помилка отримання списку кімнат", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal Server Error", "message", "Помилка отримання даних: " + e.getMessage()));
        }
    }

    // ============================================================
    // ПІДКЛЮЧЕННЯ ДО КІМНАТИ
    // ============================================================

    /**
     * 🚪 Підготовка підключення користувача до конкретної аудіокімнати.
     * (Без змін: викликається клієнтом після входу; евакуація з інших кімнат виконується всередині сервісу.)
     */
    @PostMapping("/connectclienttoroom")
    public ResponseEntity<?> connectClientToRoom(@RequestBody Map<String, Object> dataMap) {
        logger.info("Запит на підключення до кімнати");
        if (dataMap == null || !dataMap.containsKey("roomid")) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Відсутній параметр roomid"));
        }
        Number roomIdNum = (Number) dataMap.get("roomid");
        if (roomIdNum == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Параметр roomid не може бути порожнім"));
        }
        Long roomID = roomIdNum.longValue();
        try {
            Object result = radioLiveService.connectClientToAudioRoom(roomID);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Помилка підключення до кімнати {}", roomID, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Помилка підключення: " + e.getMessage()));
        }
    }

    // ============================================================
    // МОДЕРАЦІЯ ТА КЕРУВАННЯ УЧАСНИКАМИ
    // ============================================================

    /**
     * 🔄 Переміщення користувача між кімнатами через WebSocket сигналізацію.
     * 🆕 Захищено: тільки власник станції, до якої належить ЦІЛЬОВА кімната.
     *
     * @param userUuid     Токен (UUID) користувача, якого переміщуємо (participant.token зі status)
     * @param targetRoomId ID цільової кімнати (Janus room id)
     */
    @PostMapping("/moderate/move")
    public ResponseEntity<?> moveUser(@RequestParam String userUuid, @RequestParam Long targetRoomId) {
        logger.info("Модератор переміщує користувача {} в кімнату {}", userUuid, targetRoomId);
        ResponseEntity<?> denied = guardRoom(targetRoomId);
        if (denied != null) return denied;
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
     * 🆕 Захищено: тільки власник станції.
     */
    @PostMapping("/moderate/mute")
    public ResponseEntity<?> muteUser(@RequestParam Long roomId, @RequestParam Long participantId, @RequestParam boolean muted) {
        logger.info("Модератор змінює стан мікрофона для учасника {} в кімнаті {}: muted={}", participantId, roomId, muted);
        ResponseEntity<?> denied = guardRoom(roomId);
        if (denied != null) return denied;
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
     * 🆕 Захищено: тільки власник станції.
     */
    @PostMapping("/moderate/kick")
    public ResponseEntity<?> kickUser(@RequestParam Long roomId, @RequestParam Long participantId) {
        logger.info("Модератор виганяє учасника {} з кімнати {}", participantId, roomId);
        ResponseEntity<?> denied = guardRoom(roomId);
        if (denied != null) return denied;
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
     * 🆕 Захищено: тільки власник станції.
     */
    @PostMapping("/moderate/forward/start")
    public ResponseEntity<?> startForward(@RequestParam Long roomId, @RequestParam String host, @RequestParam Integer port) {
        logger.info("Модератор запускає RTP forward з кімнати {} на {}:{}", roomId, host, port);
        ResponseEntity<?> denied = guardRoom(roomId);
        if (denied != null) return denied;
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
     * 🆕 Захищено: тільки власник станції.
     */
    @PostMapping("/moderate/forward/stop")
    public ResponseEntity<?> stopForward(@RequestParam Long roomId, @RequestParam Long streamId) {
        logger.info("Модератор зупиняє RTP forward з кімнати {} з streamId {}", roomId, streamId);
        ResponseEntity<?> denied = guardRoom(roomId);
        if (denied != null) return denied;
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