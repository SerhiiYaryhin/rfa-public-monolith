package media.toloka.rfa.webrtc.controller;

import media.toloka.rfa.webrtc.model.JanusLiveStream;
import media.toloka.rfa.webrtc.service.RadioLiveService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webrtc/stream")
public class JanusLiveController {

    private final RadioLiveService radioLiveService;

    public JanusLiveController(RadioLiveService radioLiveService) {
        this.radioLiveService = radioLiveService;
    }

    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestParam String stationId, @RequestParam String streamUrl) {
        JanusLiveStream stream = radioLiveService.startBroadcast(stationId, streamUrl);
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
}
