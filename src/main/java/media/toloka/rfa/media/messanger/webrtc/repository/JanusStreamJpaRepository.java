package media.toloka.rfa.media.messanger.webrtc.repository;

import media.toloka.rfa.media.messanger.webrtc.model.JanusLiveStream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Стандартна JPA-реалізація сховища для WebRTC стрімів.
 * 
 * Spring Data JPA автоматично згенерує необхідні SQL-запити до PostgreSQL
 * на основі назв методів та сутності JanusLiveStream.
 */
@Repository
public interface JanusStreamJpaRepository extends JpaRepository<JanusLiveStream, Long>, JanusStreamRepository {

    /** Пошук за station_id (автоматичне генерування SELECT * WHERE station_id = ?) */
    @Override
    Optional<JanusLiveStream> findByStationId(String stationId);

    /** Пошук за janus_room_id */
    @Override
    Optional<JanusLiveStream> findByJanusRoomId(Long janusRoomId);

    /** Видалення за station_id (автоматичне генерування DELETE WHERE station_id = ?) */
    @Override
    void deleteByStationId(String stationId);

    /** Перевірка наявності порту (автоматичне генерування EXISTS(SELECT 1 WHERE rtp_port = ?)) */
    @Override
    boolean existsByRtpPort(Integer rtpPort);
}
