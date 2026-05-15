package media.toloka.rfa.radio.api;

import media.toloka.rfa.radio.api.dto.*;
import media.toloka.rfa.podcast.service.PodcastService;
import media.toloka.rfa.podcast.model.PodcastChannel;
import media.toloka.rfa.podcast.model.PodcastItem;
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

    private Integer parseDurationToSeconds(String duration) {
        if (duration == null || duration.isEmpty()) return 0;
        try {
            if (duration.matches("\\d+")) return Integer.valueOf(duration);
            String[] parts = duration.split(":");
            if (parts.length == 3) {
                return Integer.parseInt(parts[0]) * 3600 + Integer.parseInt(parts[1]) * 60 + Integer.parseInt(parts[2]);
            }
        } catch (Exception e) {
            return 0;
        }
        return 0;
    }

    @GetMapping("/podcasts")
    public PagedResponse<PodcastDto> getPodcasts(@RequestParam(defaultValue = "0") int page, 
                                                 @RequestParam(defaultValue = "20") int size) {
        Page<PodcastChannel> podcastPage = podcastService.GetPublicPodcastsPage(page, size);
        
        List<PodcastDto> content = podcastPage.getContent().stream()
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
        PodcastChannel podcast = podcastService.GetChanelByUUID(id);
        if (podcast == null) {
            return ResponseEntity.notFound().build();
        }

        String podcastImageUrl = podcast.getImagechanelstore() != null ? "/store/content/" + podcast.getImagechanelstore().getUuid() : null;

        List<EpisodeDto> episodes = podcast.getItem().stream()
                .map(e -> EpisodeDto.builder()
                        .id(e.getUuid())
                        .title(e.getTitle())
                        .description(e.getLead())
                        .imageUrl(e.getImagestoreitem() != null ? "/store/content/" + e.getImagestoreitem().getUuid() : podcastImageUrl)
                        .audioUrl(e.getEnclosurestore() != null ? "/store/audio/" + e.getEnclosurestore().getUuid() : null)
                        .durationSeconds(parseDurationToSeconds(e.getTimetrack()))
                        .createdAt(e.getPubDate())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(PodcastDto.builder()
                .id(podcast.getUuid())
                .title(podcast.getTitle())
                .description(podcast.getLead())
                .imageUrl(podcastImageUrl)
                .episodes(episodes)
                .build());
    }
}
