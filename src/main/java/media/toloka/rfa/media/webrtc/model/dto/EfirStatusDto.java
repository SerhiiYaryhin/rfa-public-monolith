package media.toloka.rfa.media.webrtc.model.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 📡 DTO стану прямого ефіру для панелі модератора.
 */
@Data
public class EfirStatusDto {

    private String stationUuid;
    private String stationName;
    /** Ефір активний, якщо кімната BROADCAST має roomOnlineStatus=true */
    private boolean efirActive;
    private List<RoomDto> rooms = new ArrayList<>();

    @Data
    public static class RoomDto {
        private String uuid;          // UUID кімнати чату (MessageRoom.uuid)
        private Long janusRoomId;     // Числовий ID кімнати Janus (= MessageRoom.id)
        private String name;
        private String type;          // MAIN | GREEN | BROADCAST | STAFF
        private boolean online;
        private List<ParticipantDto> participants = new ArrayList<>();
    }

    @Data
    public static class ParticipantDto {
        private Long id;              // Janus participant id (для mute/kick)
        private String display;       // Відображуване ім'я
        private String token;         // 🆕 UUID користувача (для move через /moderate/move)
        private boolean muted;
        private boolean talking;      // аудіо-індикатор "хто говорить"
        private boolean setup;
    }
}