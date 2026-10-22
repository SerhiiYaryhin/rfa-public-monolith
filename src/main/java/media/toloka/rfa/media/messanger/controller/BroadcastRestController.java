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

@Slf4j
@RestController
@RequestMapping("/api/v1/broadcast")
@RequiredArgsConstructor
public class BroadcastRestController {

    private final BroadcastRoomService broadcastRoomService;
    private final StationRepo stationRepo;
    private final ClientService clientService;

    /**
     * Ендпоінт для модератора: підготовка кімнат для прямого ефіру.
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

        // TODO: Тут варто додати перевірку прав.
        // Наприклад: if (!station.getClientdetail().getUser().getId().equals(currentUser.getId()) && !currentUser.hasRole("MODERATOR")) {
        //     return ResponseEntity.status(403).body(Map.of("error", "Недостатньо прав"));
        // }

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
}