package media.toloka.rfa.media.messanger.webrtc.model.dto;

import com.google.gson.annotations.Expose;
import lombok.Data;
import lombok.NoArgsConstructor;
import media.toloka.rfa.radio.model.enumerate.EServerState;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
public class StationDto {

    @Expose private Long id;
    @Expose private Long stationId;
    @Expose private String name;
    @Expose private String description;
    @Expose private String uuid;
    @Expose private String roomuuid;
    @Expose private Boolean stationstate;
    @Expose private EServerState serverState; // Тип Enum
    @Expose private String roomname;
    @Expose private Boolean roomOnlineStatus;
    @Expose private Date startonline;

    @Expose private List<Object> participants = new ArrayList<>();
    public List<Object> getParticipants() { return participants; }
    public void setParticipants(List<Object> participants) { this.participants = participants; }

    // Конструктор, який викликає JPQL
    public StationDto(Long id, Long stationId, String name, String description, String uuid, String roomuuid,
                      Boolean stationstate, EServerState serverState, String roomname,
                      Boolean roomOnlineStatus, Date startonline) {
        this.id = id;
        this.stationId = stationId;
        this.name = name;
        this.description = description;
        this.uuid = uuid;
        this.roomuuid = roomuuid;
        this.stationstate = stationstate;
        this.serverState = serverState;
        this.roomname = roomname;
        this.roomOnlineStatus = roomOnlineStatus;
        this.startonline = startonline;
    }
}