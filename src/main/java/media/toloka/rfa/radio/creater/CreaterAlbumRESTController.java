package media.toloka.rfa.radio.creater;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.radio.model.Album;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Track;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/creater/album")
@RequiredArgsConstructor
public class CreaterAlbumRESTController {

    private final CreaterService createrService;
    private final ClientService clientService;

    /**
     * Отримати список треків, які НЕ належать цьому альбому (для додавання)
     */
    @GetMapping("/{idAlbum}/available-tracks")
    public ResponseEntity<?> getAvailableTracks(@PathVariable Long idAlbum) {
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        List<Track> allTracks = createrService.GetAllTracksByCreater(cd);
        
        List<Map<String, Object>> available = allTracks.stream()
                .filter(t -> t.getAlbum() == null || !t.getAlbum().getId().equals(idAlbum))
                .map(t -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("uuid", t.getUuid());
                    map.put("name", t.getName());
                    map.put("autor", t.getAutor() != null ? t.getAutor() : "---");
                    return map;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(available);
    }

    /**
     * Додати трек до альбому
     */
    @PostMapping("/{idAlbum}/add-track/{uuidTrack}")
    public ResponseEntity<?> addTrackToAlbum(@PathVariable Long idAlbum, @PathVariable String uuidTrack) {
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        Album album = createrService.GetAlbumById(idAlbum);
        Track track = createrService.GetTrackByUuid(uuidTrack);

        if (album == null || track == null || !album.getClientdetail().getUuid().equals(cd.getUuid())) {
            return ResponseEntity.status(403).body("Access denied or not found");
        }

        track.setAlbum(album);
        createrService.SaveTrack(track);
        
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * Видалити трек з альбому
     */
    @PostMapping("/{idAlbum}/remove-track/{uuidTrack}")
    public ResponseEntity<?> removeTrackFromAlbum(@PathVariable Long idAlbum, @PathVariable String uuidTrack) {
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        Track track = createrService.GetTrackByUuid(uuidTrack);

        if (track == null || track.getAlbum() == null || !track.getAlbum().getId().equals(idAlbum) 
                || !track.getClientdetail().getUuid().equals(cd.getUuid())) {
            return ResponseEntity.status(403).body("Access denied or not found");
        }

        track.setAlbum(null);
        createrService.SaveTrack(track);
        
        return ResponseEntity.ok(Map.of("success", true));
    }
}
