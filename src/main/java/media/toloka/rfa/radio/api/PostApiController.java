package media.toloka.rfa.radio.api;

import media.toloka.rfa.radio.api.dto.*;
import media.toloka.rfa.radio.post.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController("apiPostController")
@RequestMapping("/api/v1")
public class PostApiController {

    @Autowired
    private PostService postService;

    @GetMapping("/posts")
    public PagedResponse<PostDto> getPosts(@RequestParam(defaultValue = "0") int page, 
                                           @RequestParam(defaultValue = "20") int size) {
        Page<media.toloka.rfa.radio.model.Post> postPage = postService.GetPostPage(page, size);
        
        var content = postPage.getContent().stream()
                .map(p -> PostDto.builder()
                        .id(p.getUuid())
                        .title(p.getPosttitle())
                        .imageUrl(p.getCoverstoreuuid() != null ? "/store/content/" + p.getCoverstoreuuid() : "")
                        .createdAt(p.getCreatedate() != null ? p.getCreatedate().toString() : "")
                        .summary(p.getLead() != null ? p.getLead() : "")
                        .build())
                .collect(Collectors.toList());

        return PagedResponse.<PostDto>builder()
                .content(content)
                .pageable(PagedResponse.PageMetadata.builder().pageNumber(page).pageSize(size).build())
                .totalPages(postPage.getTotalPages())
                .totalElements(postPage.getTotalElements())
                .last(postPage.isLast())
                .build();
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<PostDetailDto> getPostById(@PathVariable String id) {
        media.toloka.rfa.radio.model.Post p = postService.GetPostByUuid(id);
        if (p == null) {
            return ResponseEntity.notFound().build();
        }

        // Інкремент лічильника переглядів
        p.setLooked(p.getLooked() + 1);
        postService.SavePost(p);

        return ResponseEntity.ok(PostDetailDto.builder()
                .id(p.getUuid())
                .title(p.getPosttitle())
                .imageUrl(p.getCoverstoreuuid() != null ? "/store/content/" + p.getCoverstoreuuid() : "")
                .createdAt(p.getCreatedate() != null ? p.getCreatedate().toString() : "")
                .summary(p.getLead() != null ? p.getLead() : "")
                .content(p.getPostbody()) // HTML з TinyMCE
                .authorName(p.getClientdetail() != null ? p.getClientdetail().getCustname() : "Admin")
                .viewCount(p.getLooked())
                .shareUrl("https://rfa.toloka.media/post/view/" + p.getUuid())
                .build());
    }
}
