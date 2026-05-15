package media.toloka.rfa.radio.api;

import media.toloka.rfa.radio.api.dto.*;
import media.toloka.rfa.podcast.service.PodcastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController("apiPodcastController")
@RequestMapping("/api/v1")
public class PodcastApiController {

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
                        .description(p.getLead())
                        .imageUrl(p.getImagechanelstore() != null ? "/store/content/" + p.getImagechanelstore().getUuid() : null)
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

    @GetMapping("/podcasts/{id}")
    public ResponseEntity<PodcastDto> getPodcastById(@PathVariable String id) {
        var podcast = podcastService.GetChanelByUUID(id);
        if (podcast == null) {
            return ResponseEntity.notFound().build();
        }

        List<EpisodeDto> episodes = podcast.getItem().stream()
                .map(e -> EpisodeDto.builder()
                        .id(e.getUuid())
                        .title(e.getTitle())
                        .description(e.getLead())
                        .imageUrl(e.getImagestoreitem() != null ? "/store/content/" + e.getImagestoreitem().getUuid() : null)
                        .audioUrl(e.getEnclosurestore() != null ? "/store/audio/" + e.getEnclosurestore().getUuid() : null)
                        .durationSeconds(e.getTimetrack() != null ? Integer.valueOf(e.getTimetrack()) : 0)
                        .createdAt(e.getPubDate() != null ? e.getPubDate().toString() : null)
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(PodcastDto.builder()
                .id(podcast.getUuid())
                .title(podcast.getTitle())
                .description(podcast.getLead())
                .imageUrl(podcast.getImagechanelstore() != null ? "/store/content/" + podcast.getImagechanelstore().getUuid() : null)
                .episodes(episodes)
                .build());
    }
}
