package media.toloka.rfa.webrtc.controller;

import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.podcast.AdminPodcastController;
import media.toloka.rfa.webrtc.model.JanusLiveStream;
import media.toloka.rfa.webrtc.service.RadioLiveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/webrtc/stream")
public class JanusLiveController {

    private final RadioLiveService radioLiveService;

    public JanusLiveController(RadioLiveService radioLiveService) {
        this.radioLiveService = radioLiveService;
    }

    final Logger logger = LoggerFactory.getLogger(JanusLiveController.class);

    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestParam String stationId, @RequestParam String streamUrl) {
        JanusLiveStream stream = radioLiveService.startBroadcast(stationId, streamUrl);
        logger.info("Start stationId: {} ,streamUrl:{}",stationId,streamUrl);
        return ResponseEntity.ok(Map.of(
                "status", "started",
                "room_id", stream.getJanusRoomId(),
                "rtp_port", stream.getRtpPort()
        ));
    }

    @PostMapping("/stop")
    public ResponseEntity<?> stop(@RequestParam String stationId) {
        radioLiveService.stopBroadcast(stationId);
        return ResponseEntity.ok(Map.of("status", "stopped", "station_id", stationId));
    }

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        boolean ok = radioLiveService.pingJanus();
        return ResponseEntity.ok(Map.of("janus_admin_api", ok ? "up" : "down"));
    }
}
