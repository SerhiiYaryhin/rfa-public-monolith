package media.toloka.rfa.media.webrtc.service.v2;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Не використовується!!!
 *
 * 🎭 Фасад для взаємодії з Janus Gateway.
 * Забезпечує зворотну сумісність зі старим інтерфейсом JanusRabbitClientOptimized,
 * делегуючи виклики до відповідних сервісів.
 *
 * Цей клас замінює JanusRabbitClientOptimized у RadioLiveServiceV2.
 */
@Service
public class JanusGatewayFacade {

    private final JanusRoomService roomService;
    private final JanusParticipantService participantService;
    private final JanusAdminService adminService;
    private final JanusForwardingService forwardingService;
    private final JanusSessionManager sessionManager;

    public JanusGatewayFacade(
            JanusRoomService roomService,
            JanusParticipantService participantService,
            JanusAdminService adminService,
            JanusForwardingService forwardingService,
            JanusSessionManager sessionManager) {
        this.roomService = roomService;
        this.participantService = participantService;
        this.adminService = adminService;
        this.forwardingService = forwardingService;
        this.sessionManager = sessionManager;
    }

    // ============================================================
    // КІМНАТИ
    // ============================================================

    public void createAudioBridgeRoom(Long roomId, String name) {
        roomService.createAudioBridgeRoom(roomId, name);
    }

    public void destroyAudioBridgeRoom(Long janusRoomId) {
        roomService.destroyAudioBridgeRoom(janusRoomId);
    }

    public List<Map<String, Object>> getJanusRoomsAndParticipants() {
        return roomService.getJanusRoomsAndParticipants();
    }

    public List<Long> getActiveRoomsIds() {
        return roomService.getActiveRoomsIds();
    }

    public boolean isUserInAudioRoom(Long roomId, String userToken) {
        return roomService.isUserInAudioRoom(roomId, userToken);
    }

    // ============================================================
    // УЧАСНИКИ ТА МОДЕРАЦІЯ
    // ============================================================

    public Map<String, Object> joinAsRtpParticipant(
            Long sessionId, Long handleId, Long roomId, String display, String ip, Integer rtpPort) {
        return participantService.joinAsRtpParticipant(sessionId, handleId, roomId, display, ip, rtpPort);
    }

    public void kickUserFromAudioBridgeRoom(Long roomId, String userToken) {
        participantService.kickUserFromAudioBridgeRoom(roomId, userToken);
    }

    public void muteParticipant(Long roomId, Long participantId, boolean mute) {
        participantService.muteParticipant(roomId, participantId, mute);
    }

    public void kickParticipant(Long roomId, Long participantId) {
        participantService.kickParticipant(roomId, participantId);
    }

    // ============================================================
    // АДМІНІСТРУВАННЯ
    // ============================================================

    public boolean pingJanus() {
        return adminService.pingJanus();
    }

    public void registerUserToken(String token) {
        adminService.registerUserToken(token);
    }

    public boolean checkUserTokenInJanus(String token) {
        return adminService.checkUserTokenInJanus(token);
    }

    // ============================================================
    // RTP FORWARD
    // ============================================================

    public Long createRtpForward(Long roomId, String host, Integer rtpPort) {
        return forwardingService.createRtpForward(roomId, host, rtpPort);
    }

    public void stopRtpForward(Long roomId, Long streamId) {
        forwardingService.stopRtpForward(roomId, streamId);
    }

    // ============================================================
    // СЕСІЇ
    // ============================================================

    public Long createSession() {
        return sessionManager.createSession();
    }

    public Long attachPlugin(Long sessionId, String plugin) {
        return sessionManager.attachPlugin(sessionId, plugin);
    }

    public void destroySession(Long sessionId) {
        sessionManager.destroySession(sessionId);
    }

    public void keepAlive(Long sessionId) {
        sessionManager.keepAlive(sessionId);
    }

    public String getApiSecret() {
        // TODO: Додати метод отримання apiSecret з JanusAmqpClient або окремий сервіс
        return null;
    }
}