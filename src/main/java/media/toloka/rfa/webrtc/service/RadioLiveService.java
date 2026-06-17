package media.toloka.rfa.webrtc.service;

import media.toloka.rfa.webrtc.model.JanusLiveStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RadioLiveService {

    private final JanusRabbitClient janusClient;
    private final FFmpegAgentClient ffmpegClient;
    private final PortAllocationService portService;

    public RadioLiveService(JanusRabbitClient janusClient, FFmpegAgentClient ffmpegClient, PortAllocationService portService) {
        this.janusClient = janusClient;
        this.ffmpegClient = ffmpegClient;
        this.portService = portService;
    }

    @Transactional
    public JanusLiveStream startBroadcast(String stationId, String sourceStreamUrl) {
        // Якщо ефір уже активний — просто повертаємо його
        return portService.getStreamByStation(stationId).orElseGet(() -> {
            Integer freePort = portService.findNextFreePort();
            Long janusRoomId = janusClient.createDynamicRoomWithRadio("Ефір: " + stationId, freePort);
            Integer ffmpegPid = ffmpegClient.startRemoteStream(sourceStreamUrl, freePort);

            return portService.saveActiveStream(stationId, janusRoomId, freePort, ffmpegPid);
        });
    }

    @Transactional
    public void stopBroadcast(String stationId) {
        portService.getStreamByStation(stationId).ifPresent(stream -> {
            ffmpegClient.stopRemoteStream(stream.getRtpPort());
            portService.deleteStream(stationId);
            // Кімнату Janus можна не видаляти, вона згасне сама після зникнення RTP
        });
    }
}
