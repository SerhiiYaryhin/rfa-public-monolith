package media.toloka.rfa.radio.api.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class PodcastDto {
    private String uuid;
    private String title;
    private String description;
    private String coverUrl;
    private String authorName;
}
