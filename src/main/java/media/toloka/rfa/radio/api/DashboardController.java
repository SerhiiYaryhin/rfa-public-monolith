package media.toloka.rfa.radio.api;

import media.toloka.rfa.radio.api.dto.*;
import media.toloka.rfa.radio.post.service.PostService;
import media.toloka.rfa.podcast.service.PodcastService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.author.model.AuthorArticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class DashboardController {

    @Autowired
    private PostService postService;
    
    @Autowired
    private PodcastService podcastService;
    
    @Autowired
    private CreaterService createrService;

    @Cacheable(value = "dashboard", unless = "#result == null")
    @GetMapping("/dashboard")
    public DashboardDto getDashboard() {
        
        var posts = postService.GetPostPage(0, 5).getContent().stream()
                .map(p -> PostDto.builder()
                        .id(p.getUuid())
                        .title(p.getPosttitle())
                        .imageUrl(p.getCoverstoreuuid() != null ? "/store/content/" + p.getCoverstoreuuid() : "")
                        .createdAt(p.getCreatedate() != null ? p.getCreatedate().toString() : "")
                        .summary(p.getLead() != null ? p.getLead() : "")
                        .build())
                .collect(Collectors.toList());

        var columns = createrService.GetPublicAuthorArticlesPage(0, 5).getContent().stream()
                .map(c -> ColumnDto.builder()
                        .id(c.getUuid())
                        .title(c.getTitle())
                        .imageUrl(c.getCoverUuid() != null ? "/store/content/" + c.getCoverUuid() : "")
                        .createdAt(c.getPublishDate() != null ? c.getPublishDate().toString() : "")
                        .authorName(c.getColumn() != null ? c.getColumn().getTitle() : "Unknown")
                        .build())
                .collect(Collectors.toList());

        var podcasts = podcastService.GetPublicPodcastsPage(0, 5).getContent().stream()
                .map(p -> PodcastDto.builder()
                        .id(p.getUuid())
                        .title(p.getTitle())
                        .imageUrl(p.getImagechanelstore() != null ? "/store/content/" + p.getImagechanelstore().getUuid() : "")
                        .createdAt(p.getDatepublish() != null ? p.getDatepublish().toString() : "")
                        .audioUrl("") 
                        .durationSeconds(0)
                        .build())
                .collect(Collectors.toList());

        var tracks = createrService.GetLastUploadTracks().stream().limit(5)
                .map(t -> TrackDto.builder()
                        .id(t.getUuid())
                        .title(t.getName())
                        .imageUrl(t.getStoreitem() != null ? "/store/content/" + t.getStoreitem().getUuid() : "")
                        .createdAt(t.getUploaddate() != null ? t.getUploaddate().toString() : "")
                        .audioUrl(t.getStoreitem() != null ? "/store/content/" + t.getStoreitem().getUuid() : "")
                        .artist(t.getAutor())
                        .build())
                .collect(Collectors.toList());

        return DashboardDto.builder()
                .posts(posts)
                .columns(columns)
                .podcasts(podcasts)
                .tracks(tracks)
                .build();
    }
}
