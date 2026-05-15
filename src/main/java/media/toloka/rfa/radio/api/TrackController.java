package media.toloka.rfa.radio.api;

import media.toloka.rfa.radio.api.dto.*;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.model.Track;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
        
        List<TrackDto> content = trackPage.getContent().stream()
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
        TrackDto dto = new TrackDto();
        dto.setId(t.getUuid());
        dto.setTitle(t.getName());
        dto.setArtist(t.getAutor());
        dto.setStreamUrl(uuid != null ? "/store/audio/" + uuid : null);
        dto.setImageUrl(uuid != null ? "/store/content/" + uuid : null);
        dto.setCreatedAt(t.getUploaddate() != null ? t.getUploaddate().toString() : null);
        dto.setFileUrl(uuid != null ? "/store/content/" + uuid : null);
        dto.setDescription(t.getDescription());
        return dto;
    }
}
