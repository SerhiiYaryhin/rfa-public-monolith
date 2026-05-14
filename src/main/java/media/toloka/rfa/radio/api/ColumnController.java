package media.toloka.rfa.radio.api;

import media.toloka.rfa.radio.api.dto.*;
import media.toloka.rfa.author.repository.AuthorArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController("apiColumnController")
@RequestMapping("/api/v1")
public class ColumnController {

    @Autowired
    private AuthorArticleRepository authorArticleRepository;

    @GetMapping("/columns")
    public PagedResponse<ColumnDto> getColumns(@RequestParam(defaultValue = "0") int page, 
                                               @RequestParam(defaultValue = "20") int size) {
        Page<media.toloka.rfa.author.model.AuthorArticle> columnPage = authorArticleRepository.findAll(PageRequest.of(page, size));
        
        var content = columnPage.getContent().stream()
                .map(c -> ColumnDto.builder()
                        .id(c.getUuid())
                        .title(c.getTitle())
                        .imageUrl(c.getCoverUuid() != null ? "/store/content/" + c.getCoverUuid() : "")
                        .createdAt(c.getPublishDate() != null ? c.getPublishDate().toString() : "")
                        .authorName(c.getColumn() != null ? c.getColumn().getTitle() : "Unknown")
                        .build())
                .collect(Collectors.toList());

        return PagedResponse.<ColumnDto>builder()
                .content(content)
                .pageable(PagedResponse.PageMetadata.builder().pageNumber(page).pageSize(size).build())
                .totalPages(columnPage.getTotalPages())
                .totalElements(columnPage.getTotalElements())
                .last(columnPage.isLast())
                .build();
    }

    @GetMapping("/columns/{id}")
    public ResponseEntity<ColumnDetailDto> getColumnById(@PathVariable String id) {
        return authorArticleRepository.findByUuid(id)
                .map(c -> ResponseEntity.ok(ColumnDetailDto.builder()
                        .id(c.getUuid())
                        .title(c.getTitle())
                        .imageUrl(c.getCoverUuid() != null ? "/store/content/" + c.getCoverUuid() : "")
                        .createdAt(c.getPublishDate() != null ? c.getPublishDate().toString() : "")
                        .authorName(c.getColumn() != null ? c.getColumn().getTitle() : "Unknown")
                        .content(c.getPostbody()) // Raw Editor.js JSON
                        .build()))
                .orElse(ResponseEntity.notFound().build());
    }
}
