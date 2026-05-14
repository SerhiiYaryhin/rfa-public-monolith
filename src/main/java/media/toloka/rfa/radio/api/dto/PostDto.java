package media.toloka.rfa.radio.api.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class PostDto {
    private String uuid;
    private String title;
    private String authorName;
    private String coverUrl;
    private String createdAt;
}
