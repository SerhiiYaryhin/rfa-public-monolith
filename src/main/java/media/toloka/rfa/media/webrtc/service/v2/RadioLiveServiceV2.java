package media.toloka.rfa.media.webrtc.service.v2;

import media.toloka.rfa.media.webrtc.model.JanusLiveStream;
import media.toloka.rfa.media.webrtc.model.dto.StationDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

// ============================================================
// ІМПОРТИ НОВИХ СЕРВІСІВ
// ============================================================


/**
 *  Фасад для керування голосовими кімнатами та трансляцією ефіру (версія v2).
 * Делегує виклики до відповідних сервісів.
 */
@Service
public class RadioLiveServiceV2 {

    private final BroadcastLifecycleService broadcastService;
    private final RoomSynchronizationService roomSyncService;
    private final UserConnectionService userService;
    private final ModerationService moderationService;
    private final media.toloka.rfa.media.webrtc.service.v2.JanusAdminService adminService;

    public RadioLiveServiceV2(
            BroadcastLifecycleService broadcastService,
            RoomSynchronizationService roomSyncService,
            UserConnectionService userService,
            ModerationService moderationService,
            media.toloka.rfa.media.webrtc.service.v2.JanusAdminService adminService) {
        this.broadcastService = broadcastService;
        this.roomSyncService = roomSyncService;
        this.userService = userService;
        this.moderationService = moderationService;
        this.adminService = adminService;
    }

    // ============================================================
    // ІНІЦІАЛІЗАЦІЯ ТА ТРАНСЛЯЦІЇ
    // ============================================================

    public void initializeBroadcasts() {
        broadcastService.initializeBroadcasts();
    }

    public JanusLiveStream startBroadcast(String stationId) {
        return broadcastService.startBroadcast(stationId);
    }

    public void stopBroadcast(String stationId) {
        broadcastService.stopBroadcast(stationId);
    }

    // ============================================================
    // КІМНАТИ ТА СИНХРОНІЗАЦІЯ
    // ============================================================

    public List<StationDto> getActiveRoomsInfo() {
        return roomSyncService.getActiveRoomsInfo();
    }

    // ============================================================
    // ТОКЕНИ ТА ПІДКЛЮЧЕННЯ
    // ============================================================

    public String generateAndRegisterToken() {
        return userService.generateAndRegisterToken();
    }

    public Boolean checkUserTokenInJanus() {
        return userService.checkUserTokenInJanus();
    }

    public Map<String, Object> connectClientToAudioRoom(Long roomID) {
        return userService.connectClientToAudioRoom(roomID);
    }

    // ============================================================
    // МОДЕРАЦІЯ
    // ============================================================

    public void muteParticipant(Long roomId, Long participantId, boolean mute) {
        moderationService.muteParticipant(roomId, participantId, mute);
    }

    public void kickParticipant(Long roomId, Long participantId) {
        moderationService.kickParticipant(roomId, participantId);
    }

    public Long startRtpForward(Long roomId, String host, Integer port) {
        return moderationService.startRtpForward(roomId, host, port);
    }

    public void stopRtpForward(Long roomId, Long streamId) {
        moderationService.stopRtpForward(roomId, streamId);
    }

    // ============================================================
    // ДІАГНОСТИКА
    // ============================================================

    public boolean pingJanus() {
        return adminService.pingJanus();
    }

    public String getApiSecret() {
        return "janusrocks"; // Значення з application-chat.properties
    }
}