package media.toloka.rfa.radio.api.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class PostDto {
    private String id;
    private String title;
    private String imageUrl;
    private String createdAt;
    private String summary;
}
