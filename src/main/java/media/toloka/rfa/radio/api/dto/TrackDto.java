package media.toloka.rfa.radio.api.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TrackDto {
    private String id;
    private String title;
    private String artist;
    private String streamUrl;
    private String genre;
    private String imageUrl;
    private String description;
    private String fileUrl;
    private String createdAt;
}
