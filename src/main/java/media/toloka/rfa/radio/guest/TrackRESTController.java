package media.toloka.rfa.radio.guest;

import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.model.Track;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/1.0/track")
public class TrackRESTController {

    @Autowired
    private CreaterService createrService;

    @PostMapping("/{uuid}/inc-looked")
    public ResponseEntity<?> incrementTrackLooked(@PathVariable String uuid) {
        Track track = createrService.GetTrackByUuid(uuid);
        if (track == null) {
            // Спробуємо знайти за UUID сховища, якщо це було посилання на файл
            track = createrService.GetTrackByStoreuuid(uuid);
        }
        
        if (track == null) {
            return ResponseEntity.notFound().build();
        }
        
        createrService.incrementTrackLooked(track);
        return ResponseEntity.ok(Map.of("success", true, "looked", track.getLooked()));
    }
}
