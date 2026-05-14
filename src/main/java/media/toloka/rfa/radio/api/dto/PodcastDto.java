package media.toloka.rfa.radio.api.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class PodcastDto {
    private String id;
    private String title;
    private String imageUrl;
    private String createdAt;
    private String audioUrl;
    private Integer durationSeconds;
}
