package media.toloka.rfa.radio.api.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class EpisodeDto {
    private String id;
    private String title;
    private String description;
    private String imageUrl;
    private String audioUrl;
    private Integer durationSeconds;
    private String createdAt;
}
