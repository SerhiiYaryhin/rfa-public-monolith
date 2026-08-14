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
import org.springframework.web.multipart.MultipartFile;

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

    private Clientdetail getCurrentUser() {
        return clientService.GetClientDetailByUser(clientService.GetCurrentUser());
    }

    @GetMapping("/{albumUuid}/available-tracks")
    public ResponseEntity<?> getAvailableTracks(@PathVariable String albumUuid) {
        Clientdetail cd = getCurrentUser();
        if (cd == null) return ResponseEntity.status(401).build();

        Album album = createrService.GetAlbumByUuid(albumUuid);
        if (album == null || !album.getClientdetail().getUuid().equals(cd.getUuid())) {
            return ResponseEntity.status(403).body("Access denied or not found");
        }

        List<Track> allTracks = createrService.GetAllTracksByCreater(cd);
        List<Map<String, Object>> available = allTracks.stream()
                .filter(t -> t.getAlbum() == null || !t.getAlbum().getUuid().equals(albumUuid))
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

    @PostMapping("/{albumUuid}/add-track/{uuidTrack}")
    public ResponseEntity<?> addTrackToAlbum(@PathVariable String albumUuid, @PathVariable String uuidTrack) {
        Clientdetail cd = getCurrentUser();
        if (cd == null) return ResponseEntity.status(401).build();

        Album album = createrService.GetAlbumByUuid(albumUuid);
        Track track = createrService.GetTrackByUuid(uuidTrack);

        if (album == null || track == null || !album.getClientdetail().getUuid().equals(cd.getUuid())) {
            return ResponseEntity.status(403).body("Access denied or not found");
        }

        // Додаткова перевірка, що трек належить цьому ж користувачу
        if (!track.getClientdetail().getUuid().equals(cd.getUuid())) {
            return ResponseEntity.status(403).body("Track does not belong to user");
        }

        track.setAlbum(album);
        createrService.SaveTrack(track);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/{albumUuid}/remove-track/{uuidTrack}")
    public ResponseEntity<?> removeTrackFromAlbum(@PathVariable String albumUuid, @PathVariable String uuidTrack) {
        Clientdetail cd = getCurrentUser();
        if (cd == null) return ResponseEntity.status(401).build();

        Album album = createrService.GetAlbumByUuid(albumUuid);
        Track track = createrService.GetTrackByUuid(uuidTrack);

        if (album == null || track == null || track.getAlbum() == null
                || !track.getAlbum().getUuid().equals(albumUuid)
                || !track.getClientdetail().getUuid().equals(cd.getUuid())) {
            return ResponseEntity.status(403).body("Access denied or not found");
        }

        track.setAlbum(null);
        createrService.SaveTrack(track);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * Ендпоінт для завантаження нової обкладинки альбому (викликається з editalbum.html)
     */
    @PostMapping("/{albumUuid}/replace-cover")
    public ResponseEntity<?> replaceAlbumCover(
            @PathVariable String albumUuid,
            @RequestParam("file") MultipartFile file) {
        Clientdetail cd = getCurrentUser();
        if (cd == null) return ResponseEntity.status(401).build();

        Album album = createrService.GetAlbumByUuid(albumUuid);
        if (album == null || !album.getClientdetail().getUuid().equals(cd.getUuid())) {
            return ResponseEntity.status(403).body("Access denied");
        }

        // TODO: Реалізувати збереження файлу через FilesService або StoreService.
        // Приклад: Store newCover = storeService.saveAlbumCover(file, cd);
        // album.setStoreuuidalbumcover(newCover.getUuid());
        // createrService.SaveAlbum(album);
        // return ResponseEntity.ok(Map.of("success", true, "url", "/store/content/" + newCover.getUuid()));

        log.warn("Method replaceAlbumCover called but file saving logic is not implemented yet.");
        return ResponseEntity.ok(Map.of("success", false, "message", "Завантаження обкладинки через цей API поки не реалізовано на бекенді."));
    }
}