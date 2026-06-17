package media.toloka.rfa.webrtc.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Сутність для збереження стану активних динамічних кімнат та RTP-портів у базі даних.
 * Забезпечує захист від конфліктів портів при перезапуску серверів RFA.
 */
@Entity
@Table(name = "janus_live_streams")
public class JanusLiveStream {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Унікальний ідентифікатор радіостанції (наприклад, "toloka-radio", "rfa-rock")
    @Column(name = "station_id", nullable = false, unique = true, length = 50)
    private String stationId;

    // ID кімнати, який динамічно повернув Janus медіасервер
    @Column(name = "janus_room_id", nullable = false)
    private Long janusRoomId;

    // Динамічний UDP порт, виділений під FFmpeg потік цієї станції
    @Column(name = "rtp_port", nullable = false, unique = true)
    private Integer rtpPort;

    // Process ID (PID) запущеного FFmpeg процесу на віддаленому сервері трансляцій
    @Column(name = "ffmpeg_pid")
    private Integer ffmpegPid;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Автоматично виставляємо час створення перед збереженням в БД
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // --- КОНСТРУКТОРИ ---
    public JanusLiveStream() {
    }

    public JanusLiveStream(String stationId, Long janusRoomId, Integer rtpPort, Integer ffmpegPid) {
        this.stationId = stationId;
        this.janusRoomId = janusRoomId;
        this.rtpPort = rtpPort;
        this.ffmpegPid = ffmpegPid;
    }

    // --- ГЕТЕРИ ТА СЕТЕРИ ---
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

    public LocalDateTime getCreatedAt() { return createdAt; }
}
