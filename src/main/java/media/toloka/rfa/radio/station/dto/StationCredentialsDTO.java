package media.toloka.rfa.radio.station.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StationCredentialsDTO {
    private Long id;
    private String username;
    private String password;
}
