package media.toloka.rfa.media.messanger.webrtc.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Сутність (Entity) для збереження стану активних WebRTC трансляцій у базі даних.
 * 
 * Цей об'єкт відіграє ключову роль у запобіганні конфліктам ресурсів:
 * 1. Забезпечує унікальність UDP портів (rtpPort).
 * 2. Дозволяє Spring Boot знати, яка радіостанція наразі має відкриту кімнату в Janus.
 * 3. Зберігає інформацію про процес FFmpeg (ffmpegPid) для можливості його примусової зупинки.
 */
@Entity
@Table(name = "janus_live_streams")
public class JanusLiveStream {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Унікальний ідентифікатор радіостанції.
     * Наприклад: "rock-fm", "toloka-news".
     */
    @Column(name = "station_id", nullable = false, unique = true, length = 36)
    private String stationId;

    /**
     * Числовий ID кімнати, який динамічно згенерував Janus медіасервер.
     * Необхідний фронтенду для підключення через WebRTC.
     */
    @Column(name = "janus_room_id", nullable = false)
    private Long janusRoomId;

    /**
     * UDP порт на сервері Janus (janus.rfa), виділений для прийому аудіо від FFmpeg.
     * Кожна станція повинна мати унікальний порт у межах сервера.
     */
    @Column(name = "rtp_port", nullable = false, unique = true)
    private Integer rtpPort;

    /**
     * Ідентифікатор процесу FFmpeg на віддаленому сервері трансляцій.
     * Використовується для моніторингу та зупинки процесу перекодування.
     */
    @Column(name = "ffmpeg_pid")
    private Integer ffmpegPid;

    /**
     * Час створення запису. Використовується для аналітики та очищення старих "завислих" сесій.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Колбек Hibernate для автоматичного виставлення дати створення перед збереженням у БД.
     */
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
