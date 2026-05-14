package media.toloka.rfa.radio.api;

import media.toloka.rfa.radio.api.dto.*;
import media.toloka.rfa.radio.creater.service.CreaterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
        Page<media.toloka.rfa.radio.model.Track> trackPage = createrService.GetTrackPage(page, size);
        
        var content = trackPage.getContent().stream()
                .map(t -> TrackDto.builder()
                        .id(t.getUuid())
                        .title(t.getName())
                        .imageUrl(t.getStoreitem() != null ? "/store/content/" + t.getStoreitem().getUuid() : "")
                        .createdAt(t.getUploaddate() != null ? t.getUploaddate().toString() : "")
                        .audioUrl(t.getStoreitem() != null ? "/store/content/" + t.getStoreitem().getUuid() : "")
                        .artist(t.getAutor())
                        .build())
                .collect(Collectors.toList());

        return PagedResponse.<TrackDto>builder()
                .content(content)
                .pageable(PagedResponse.PageMetadata.builder().pageNumber(page).pageSize(size).build())
                .totalPages(trackPage.getTotalPages())
                .totalElements(trackPage.getTotalElements())
                .last(trackPage.isLast())
                .build();
    }
}
