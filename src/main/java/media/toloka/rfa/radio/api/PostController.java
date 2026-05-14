package media.toloka.rfa.radio.api;

import media.toloka.rfa.radio.api.dto.*;
import media.toloka.rfa.radio.post.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class PostController {

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
}
