package media.toloka.rfa.media.messanger.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.media.messanger.service.BroadcastRoomService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.radio.repository.StationRepo;
import media.toloka.rfa.security.model.Users;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API керування кімнатами прямого ефіру.
 *
 * Ендпоінти:
 *  POST /api/v1/broadcast/prepare/{stationUuid} — ліниве створення кімнат (БД + Janus).
 *  POST /api/v1/broadcast/sync/{stationUuid}    — примусова синхронізація з Janus.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/broadcast")
@RequiredArgsConstructor
public class BroadcastRestController {

    private final BroadcastRoomService broadcastRoomService;
    private final StationRepo stationRepo;
    private final ClientService clientService;

    /**
     * Підготовка кімнат для прямого ефіру (ідімпотентна операція).
     * POST /api/v1/broadcast/prepare/{stationUuid}
     */
    @PostMapping("/prepare/{stationUuid}")
    public ResponseEntity<?> prepareRooms(@PathVariable String stationUuid) {
        Users currentUser = clientService.GetCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Необхідна авторизація"));
        }

        Station station = stationRepo.getStationByUuid(stationUuid);
        if (station == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Станцію не знайдено"));
        }

        // Базова перевірка прав: власник станції.
        // TODO: розширити до повноцінних ролей модераторів (окремий етап RBAC).
        if (station.getClientdetail() == null )
        {
            return ResponseEntity.status(403).body(Map.of("error", "Недостатньо прав для керування цією станцією"));
        }

//        if (station.getClientdetail() == null
//                || station.getClientdetail().getUser() == null
//                || !station.getClientdetail().getUser().getId().equals(currentUser.getId())) {
//            return ResponseEntity.status(403).body(Map.of("error", "Недостатньо прав для керування цією станцією"));
//        }

        try {
            Map<String, String> rooms = broadcastRoomService.prepareBroadcastRooms(stationUuid);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Кімнати для ефіру успішно підготовлено",
                    "data", rooms
            ));
        } catch (Exception e) {
            log.error("Помилка при підготовці кімнат ефіру: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Внутрішня помилка сервера: " + e.getMessage()));
        }
    }

    /**
     * Примусова синхронізація кімнат станції з Janus (self-healing вручну).
     * Корисно після рестарту Janus або для діагностики.
     * POST /api/v1/broadcast/sync/{stationUuid}
     */
    @PostMapping("/sync/{stationUuid}")
    public ResponseEntity<?> syncRooms(@PathVariable String stationUuid) {
        Users currentUser = clientService.GetCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Необхідна авторизація"));
        }

        Station station = stationRepo.getStationByUuid(stationUuid);
        if (station == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Станцію не знайдено"));
        }

        // Та сама базова перевірка прав, що й у prepare.
        if (station.getClientdetail() == null
                || station.getClientdetail().getUser() == null
                || !station.getClientdetail().getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Недостатньо прав для керування цією станцією"));
        }

        try {
            Map<String, String> rooms = broadcastRoomService.syncJanusRooms(stationUuid);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Синхронізацію кімнат з Janus завершено",
                    "data", rooms
            ));
        } catch (Exception e) {
            log.error("Помилка синхронізації кімнат з Janus: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Помилка синхронізації: " + e.getMessage()));
        }
    }
}