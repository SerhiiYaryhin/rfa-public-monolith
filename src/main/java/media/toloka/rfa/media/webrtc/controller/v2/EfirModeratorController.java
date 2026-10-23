package media.toloka.rfa.media.webrtc.controller.v2;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.media.webrtc.service.v2.EfirModerationService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.radio.repository.StationRepo;
import media.toloka.rfa.security.model.ERole;
import media.toloka.rfa.security.model.Users;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 🎛️ REST API панелі модератора прямого ефіру.
 *
 * Ендпоінти:
 * GET  /api/v1/broadcast/{stationUuid}/status            — стан кімнат, учасників, аудіо-індикатори
 * POST /api/v1/broadcast/{stationUuid}/moderation/mute   — mute/unmute учасника
 * POST /api/v1/broadcast/{stationUuid}/moderation/kick   — вигнати учасника
 * POST /api/v1/broadcast/{stationUuid}/moderation/move   — команда клієнту перейти в іншу кімнату
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/broadcast")
@RequiredArgsConstructor
public class EfirModeratorController {

    private final EfirModerationService efirModerationService;
    private final ClientService clientService;
    private final StationRepo stationRepo;

    // ---------------- STATUS ----------------

    @GetMapping("/{stationUuid}/status")
    public ResponseEntity<?> status(@PathVariable String stationUuid) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Необхідна авторизація"));
        }

        Station station = stationRepo.getStationByUuid(stationUuid);
        if (station == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Станцію не знайдено"));
        }

        // Перевірка прав: власник АБО адмін/модератор
        boolean isOwner = station.getClientdetail() != null
                && station.getClientdetail().getUser() != null
                && station.getClientdetail().getUser().getId().equals(user.getId());

        boolean hasAdminRole = user.getRoles().stream()
                .anyMatch(r -> r.getRole() == ERole.ROLE_ADMIN || r.getRole() == ERole.ROLE_MODERATOR);

        if (!isOwner && !hasAdminRole) {
            return ResponseEntity.status(403).body(Map.of("error", "Недостатньо прав для цієї станції"));
        }

        try {
            return ResponseEntity.ok(efirModerationService.getStatus(stationUuid));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Помилка отримання статусу ефіру: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ---------------- MODERATION ----------------

    @PostMapping("/{stationUuid}/moderation/mute")
    public ResponseEntity<?> mute(@PathVariable String stationUuid, @RequestBody MuteRequest req) {
        ResponseEntity<?> guard = guard(stationUuid);
        if (guard != null) return guard;

        try {
            efirModerationService.mute(stationUuid, req.getRoomId(), req.getParticipantId(), req.isMuted());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Помилка mute: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{stationUuid}/moderation/kick")
    public ResponseEntity<?> kick(@PathVariable String stationUuid, @RequestBody KickRequest req) {
        ResponseEntity<?> guard = guard(stationUuid);
        if (guard != null) return guard;

        try {
            efirModerationService.kick(stationUuid, req.getRoomId(), req.getParticipantId());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Помилка kick: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{stationUuid}/moderation/move")
    public ResponseEntity<?> move(@PathVariable String stationUuid, @RequestBody MoveRequest req) {
        ResponseEntity<?> guard = guard(stationUuid);
        if (guard != null) return guard;

        try {
            efirModerationService.move(stationUuid, req.getParticipantId(), req.getTargetRoomUuid());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Помилка move: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ---------------- GUARD / DTO ----------------

    /**
     * Базова перевірка: користувач авторизований і є власником станції АБО має роль ADMIN/MODERATOR.
     */
    private ResponseEntity<?> guard(String stationUuid) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Необхідна авторизація"));
        }

        Station station = stationRepo.getStationByUuid(stationUuid);
        if (station == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Станцію не знайдено"));
        }

        // Перевірка власника
        boolean isOwner = station.getClientdetail() != null
                && station.getClientdetail().getUser() != null
                && station.getClientdetail().getUser().getId().equals(user.getId());

        // Перевірка ролей ADMIN або MODERATOR
        boolean hasAdminRole = user.getRoles().stream()
                .anyMatch(r -> r.getRole() == ERole.ROLE_ADMIN || r.getRole() == ERole.ROLE_MODERATOR);

        if (!isOwner && !hasAdminRole) {
            return ResponseEntity.status(403).body(Map.of("error", "Недостатньо прав для керування цією станцією"));
        }

        return null; // Доступ дозволено
    }

    @Data
    public static class MuteRequest {
        private Long roomId;          // Janus room id (= MessageRoom.id)
        private Long participantId;   // Janus participant id
        private boolean muted;        // true = вимкнути мікрофон
    }

    @Data
    public static class KickRequest {
        private Long roomId;
        private Long participantId;
    }

    @Data
    public static class MoveRequest {
        private Long participantId;
        private String targetRoomUuid; // UUID цільової кімнати чату
    }
}