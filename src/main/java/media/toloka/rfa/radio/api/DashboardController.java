package media.toloka.rfa.radio.api;

import media.toloka.rfa.radio.api.dto.*;
import media.toloka.rfa.radio.post.service.PostService;
import media.toloka.rfa.podcast.service.PodcastService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
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

    @GetMapping("/dashboard")
    public DashboardDto getDashboard() {
        // За замовчуванням беремо 5 елементів
        
        var posts = postService.GetPostPage(0, 5).getContent().stream()
                .map(p -> PostDto.builder()
                        .uuid(p.getUuid())
                        .title(p.getPosttitle())
                        .authorName(p.getClientdetail() != null ? p.getClientdetail().getCustname() : "Admin")
                        .createdAt(p.getCreatedate() != null ? p.getCreatedate().toString() : "")
                        .build())
                .collect(Collectors.toList());

        var podcasts = podcastService.GetPodcastsByClientPage(null, 0, 5).getContent().stream()
                .map(p -> PodcastDto.builder()
                        .uuid(p.getUuid())
                        .title(p.getTitle())
                        .description(p.getLead())
                        .authorName(p.getUuid()) // Поле authorName в PodcastChannel відсутнє, використовуємо uuid як тимчасове рішення
                        .build())
                .collect(Collectors.toList());

        var tracks = createrService.GetLastUploadTracks().stream().limit(5)
                .map(t -> TrackDto.builder()
                        .uuid(t.getUuid())
                        .name(t.getName())
                        .autor(t.getAutor())
                        .albumName(t.getAlbum() != null ? t.getAlbum().getName() : "Single")
                        .audioUrl(t.getStoreitem() != null ? "/store/content/" + t.getStoreitem().getUuid() : "")
                        .build())
                .collect(Collectors.toList());


        return DashboardDto.builder()
                .posts(posts)
                .podcasts(podcasts)
                .tracks(tracks)
                .build();
    }
}
