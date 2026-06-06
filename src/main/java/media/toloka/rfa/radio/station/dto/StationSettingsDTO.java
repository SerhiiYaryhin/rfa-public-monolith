package media.toloka.rfa.radio.station.dto;

import lombok.Data;

@Data
public class StationSettingsDTO {
    private Long id;
    private String name;
    private String icecastName;
    private String icecastDescription;
    private String icecastGenre;
    private String icecastSite;
    private String adminPassword;
}
