package media.toloka.rfa.radio.api.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class DashboardDto {
    private List<PostDto> posts;
    private List<PodcastDto> podcasts;
    private List<TrackDto> tracks;
}
