package media.toloka.rfa.media.messanger.webrtc.controller;

import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.media.messanger.model.MessageRoom;
import media.toloka.rfa.media.messanger.model.enumerate.EChatRoomType;
import media.toloka.rfa.media.messanger.repository.MessageRoomRepository;
import media.toloka.rfa.media.messanger.service.MessangerService;
import media.toloka.rfa.media.messanger.webrtc.model.JanusLiveStream;
import media.toloka.rfa.media.messanger.webrtc.service.RadioLiveService;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.radio.station.service.StationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST-контролер для керування живими WebRTC трансляціями.
 * 
 * Цей контролер надає зовнішній API для запуску та зупинки ефірів радіостанцій.
 * Він взаємодіє з RadioLiveService, який координує роботу медіасервера Janus
 * та FFmpeg агентів.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/webrtc/stream")
public class JanusLiveController {

    private final RadioLiveService radioLiveService;

    public JanusLiveController(RadioLiveService radioLiveService) {
        this.radioLiveService = radioLiveService;
    }

    private static final Logger logger = LoggerFactory.getLogger(JanusLiveController.class);

    @Autowired
    private MessangerService messangerService;

    @Autowired
    private StationService stationService;

    /**
     * Запуск трансляції радіостанції в WebRTC.
     * 
     * @param stationId Унікальний ID станції (наприклад, "rfa-rock")
     * @param streamUrl Посилання на Icecast потік джерела (наприклад, "http://icecast.rfa/rock.mp3")
     * @return Дані про створену кімнату: ID кімнати Janus та виділений RTP порт
     */
    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestParam String stationId, @RequestParam String streamUrl) {
        logger.info("Запит на запуск стріму: stationId={}, streamUrl={}", stationId, streamUrl);
        JanusLiveStream stream = radioLiveService.startBroadcast(stationId, streamUrl);
        
        return ResponseEntity.ok(Map.of(
                "status", "started",
                "room_id", stream.getJanusRoomId(),
                "rtp_port", stream.getRtpPort(),
                "station_id", stationId
        ));
    }

    /**
     * Зупинка активної трансляції.
     * Припиняє роботу FFmpeg процесу та видаляє кімнату в Janus (автоматично за таймаутом або явно).
     * 
     * @param stationId ID станції, яку потрібно вимкнути
     */
    @PostMapping("/stop")
    public ResponseEntity<?> stop(@RequestParam String stationId) {
        logger.info("Запит на зупинку стріму: stationId={}", stationId);
        radioLiveService.stopBroadcast(stationId);
        return ResponseEntity.ok(Map.of("status", "stopped", "station_id", stationId));
    }

    @GetMapping("/getonlinestation")
    public ResponseEntity<?> getOnlinesSation () {
        //  Отримуємо перелік кімнат станцій, які зараз в онлайні з бази станцій
        List<Station> onlineStations = stationService.getStationsWithOnlineRooms();
        // отримуємо перелік аудіокімнат з Janus
//        List<Map<String, Object>> janusRoomsList = radioLiveService.getActiveRoomsInfo();
        // Порівнюємо. За необхідністю створюємо або знищуємо кімнати в Янусі

        // повертаємо актуальний перелік кімнат радіостанцій

        return ResponseEntity.ok(onlineStations);
    }

    /**
     * Тестовий ендпоінт для перевірки зв'язку з Janus Admin API.
     * Допомагає діагностувати проблеми з RabbitMQ та авторизацією.
     */
    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        boolean ok = radioLiveService.pingJanus();
        return ResponseEntity.ok(Map.of(
            "janus_admin_api", ok ? "up" : "down",
            "message", ok ? "Зв'язок з Janus встановлено успішно" : "Помилка зв'язку (перевірте RabbitMQ та секрети)"
        ));
    }

    /**
     * Отримання динамічного токена для WebRTC підключення.
     */
    @GetMapping("/token")
    public ResponseEntity<?> getToken() {
        String token = radioLiveService.generateAndRegisterToken();
        // Якщо користувач не авторизований або cd == null
        if (token == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error", "Unauthorized",
                            "message", "Користувач повинен увійти в систему для користування чатом."
                    ));
        }
        String apiSecret = radioLiveService.getApiSecret();
        return ResponseEntity.ok(Map.of(
            "token", token,
            "api_secret", apiSecret
        ));
    }

    /**
     * Отримання списку активних кімнат, їхніх учасників та статусу трансляції.
     */
    @GetMapping("/rooms")
    public ResponseEntity<?> getRooms() {
        logger.info("Запит на отримання списку кімнат та користувачів");
//        return ResponseEntity.ok(radioLiveService.getActiveRoomsInfo());
        return ResponseEntity.ok(radioLiveService.getActiveRoomsInfo());
    }
}
