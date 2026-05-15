package media.toloka.rfa.radio.api;

import media.toloka.rfa.radio.api.dto.*;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.model.Track;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController("apiTrackController")
@RequestMapping("/api/v1")
public class TrackController {

    @Autowired
    private CreaterService createrService;

    @GetMapping("/tracks")
    public PagedResponse<TrackDto> getTracks(@RequestParam(defaultValue = "0") int page, 
                                             @RequestParam(defaultValue = "20") int size) {
        Page<Track> trackPage = createrService.GetTrackPage(page, size);
        
        var content = trackPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<TrackDto>builder()
                .content(content)
                .pageable(PagedResponse.PageMetadata.builder().pageNumber(page).pageSize(size).build())
                .totalPages(trackPage.getTotalPages())
                .totalElements(trackPage.getTotalElements())
                .last(trackPage.isLast())
                .build();
    }

    @GetMapping("/tracks/{id}")
    public ResponseEntity<TrackDto> getTrackById(@PathVariable String id) {
        Track track = createrService.GetTrackByUuid(id);
        if (track == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapToDto(track));
    }

    private TrackDto mapToDto(Track t) {
        String uuid = t.getStoreitem() != null ? t.getStoreitem().getUuid() : null;
        return TrackDto.builder()
                .id(t.getUuid())
                .title(t.getName())
                .artist(t.getAutor())
                .streamUrl(uuid != null ? "/store/audio/" + uuid : null)
                .imageUrl(uuid != null ? "/store/content/" + uuid : null)
                .createdAt(t.getUploaddate() != null ? t.getUploaddate().toString() : null)
                .fileUrl(uuid != null ? "/store/content/" + uuid : null)
                .description(t.getDescription())
                .build();
    }
}
