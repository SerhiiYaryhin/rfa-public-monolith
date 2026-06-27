package media.toloka.rfa.media.webrtc.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class FFmpegAgentClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ffmpeg.agent.url:http://localhost:8000}")
    private String agentUrl;

    @Value("${janus.host.internal:localhost}")
    private String janusHost;

    public Integer startRemoteStream(String streamUrl, Integer rtpPort) {
        String url = agentUrl + "/api/stream/start";
        Map<String, Object> request = Map.of(
                "stream_url", streamUrl,
                "janus_host", janusHost,
                "rtp_port", rtpPort
        );
        Map<?, ?> response = restTemplate.postForObject(url, request, Map.class);
        return response != null ? ((Number) response.get("pid")).intValue() : null;
    }

    public void stopRemoteStream(Integer rtpPort) {
        String url = agentUrl + "/api/stream/stop/" + rtpPort;
        restTemplate.postForObject(url, null, Map.class);
    }
}