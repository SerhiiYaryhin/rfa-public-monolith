package media.toloka.rfa.media.webrtc.service.v2;

import org.springframework.stereotype.Service;

/**
 * 🎛️ Сервіс модерації аудіокімнат (делегує виклики).
 */
@Service
public class ModerationService {

    private final JanusParticipantService participantService;
    private final JanusForwardingService forwardingService;

    public ModerationService(
            JanusParticipantService participantService,
            JanusForwardingService forwardingService) {
        this.participantService = participantService;
        this.forwardingService = forwardingService;
    }

    public void muteParticipant(Long roomId, Long participantId, boolean mute) {
        participantService.muteParticipant(roomId, participantId, mute);
    }

    public void kickParticipant(Long roomId, Long participantId) {
        participantService.kickParticipant(roomId, participantId);
    }

    public Long startRtpForward(Long roomId, String host, Integer port) {
        return forwardingService.createRtpForward(roomId, host, port);
    }

    public void stopRtpForward(Long roomId, Long streamId) {
        forwardingService.stopRtpForward(roomId, streamId);
    }
}