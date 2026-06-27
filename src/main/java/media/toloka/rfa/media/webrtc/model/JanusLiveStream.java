package media.toloka.rfa.media.webrtc.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "janus_live_streams")
public class JanusLiveStream {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "station_id", nullable = false, unique = true)
    private String stationId;

    @Column(name = "janus_room_id", nullable = false)
    private Long janusRoomId;

    @Column(name = "rtp_port", nullable = false)
    private Integer rtpPort;

    @Column(name = "ffmpeg_pid")
    private Integer ffmpegPid;

    // ✅ ID RTP forward для керування потоком
    @Column(name = "rtp_forward_stream_id")
    private Long rtpForwardStreamId;

    // ✅ НОВЕ ПОЛЕ: Дата створення запису
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public JanusLiveStream() {}

    public JanusLiveStream(String stationId, Long janusRoomId, Integer rtpPort, Integer ffmpegPid) {
        this.stationId = stationId;
        this.janusRoomId = janusRoomId;
        this.rtpPort = rtpPort;
        this.ffmpegPid = ffmpegPid;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }

    public Long getJanusRoomId() { return janusRoomId; }
    public void setJanusRoomId(Long janusRoomId) { this.janusRoomId = janusRoomId; }

    public Integer getRtpPort() { return rtpPort; }
    public void setRtpPort(Integer rtpPort) { this.rtpPort = rtpPort; }

    public Integer getFfmpegPid() { return ffmpegPid; }
    public void setFfmpegPid(Integer ffmpegPid) { this.ffmpegPid = ffmpegPid; }

    public Long getRtpForwardStreamId() { return rtpForwardStreamId; }
    public void setRtpForwardStreamId(Long rtpForwardStreamId) { this.rtpForwardStreamId = rtpForwardStreamId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}