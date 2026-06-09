package media.toloka.rfa.radio.station.controllerREST;

import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Contract;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.radio.model.enumerate.EContractStatus;
import media.toloka.rfa.radio.station.service.StationService;
import media.toloka.rfa.security.model.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static media.toloka.rfa.rpc.model.ERPCJobType.*;

@RestController
@RequestMapping("/api/1.0/user/station")
public class StationRESTController {

    @Autowired
    private StationService stationService;

    @Autowired
    private ClientService clientService;

    @PostMapping("/{id}/start")
    public ResponseEntity<?> startStation(@PathVariable Long id) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        Station station = stationService.GetStationById(id);
        if (station == null) return ResponseEntity.notFound().build();

        // Перевірка прав
        if (!station.getClientdetail().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        Contract contract = station.getContract();
        if (contract == null || (contract.getClientdetail().getAccount() < 0 && contract.getContractStatus() == EContractStatus.CONTRACT_PAY)) {
            return ResponseEntity.badRequest().body("Недостатньо коштів або відсутній контракт.");
        }

        stationService.triggerJob(station, user, JOB_STATION_START);
        return ResponseEntity.ok(Map.of("success", true, "message", "Запит на запуск відправлено"));
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<?> stopStation(@PathVariable Long id) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        Station station = stationService.GetStationById(id);
        if (station == null) return ResponseEntity.notFound().build();

        if (!station.getClientdetail().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        stationService.triggerJob(station, user, JOB_STATION_STOP);
        return ResponseEntity.ok(Map.of("success", true, "message", "Запит на зупинку відправлено"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStation(@PathVariable Long id) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        Station station = stationService.GetStationById(id);
        if (station == null) return ResponseEntity.notFound().build();

        if (!station.getClientdetail().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        stationService.triggerJob(station, user, JOB_STATION_DELETE);
        return ResponseEntity.ok(Map.of("success", true, "message", "Запит на видалення відправлено"));
    }
}
