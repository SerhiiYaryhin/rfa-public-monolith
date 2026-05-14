package media.toloka.rfa.radio.api.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class TrackDto {
    private String uuid;
    private String name;
    private String autor;
    private String albumName;
    private String audioUrl;
    private String coverUrl;
}
