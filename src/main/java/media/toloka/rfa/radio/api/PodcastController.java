package media.toloka.rfa.radio.api;

import media.toloka.rfa.radio.api.dto.*;
import media.toloka.rfa.podcast.service.PodcastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class PodcastController {

    @Autowired
    private PodcastService podcastService;

    @GetMapping("/podcasts")
    public PagedResponse<PodcastDto> getPodcasts(@RequestParam(defaultValue = "0") int page, 
                                                 @RequestParam(defaultValue = "20") int size) {
        Page<media.toloka.rfa.podcast.model.PodcastChannel> podcastPage = podcastService.GetPublicPodcastsPage(page, size);
        
        var content = podcastPage.getContent().stream()
                .map(p -> PodcastDto.builder()
                        .id(p.getUuid())
                        .title(p.getTitle())
                        .imageUrl(p.getImagechanelstore() != null ? "/store/content/" + p.getImagechanelstore().getUuid() : "")
                        .createdAt(p.getDatepublish() != null ? p.getDatepublish().toString() : "")
                        .audioUrl("")
                        .durationSeconds(0)
                        .build())
                .collect(Collectors.toList());

        return PagedResponse.<PodcastDto>builder()
                .content(content)
                .pageable(PagedResponse.PageMetadata.builder().pageNumber(page).pageSize(size).build())
                .totalPages(podcastPage.getTotalPages())
                .totalElements(podcastPage.getTotalElements())
                .last(podcastPage.isLast())
                .build();
    }
}
