package media.toloka.rfa.radio.api.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class PostDetailDto {
    private String id;
    private String title;
    private String imageUrl;
    private String createdAt;
    private String summary;
    private String content; // HTML з TinyMCE
    private String authorName;
    private Long viewCount;
    private String shareUrl;
}
