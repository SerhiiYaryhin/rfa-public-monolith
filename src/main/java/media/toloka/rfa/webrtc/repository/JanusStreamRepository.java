package media.toloka.rfa.webrtc.repository;

import media.toloka.rfa.webrtc.model.JanusLiveStream;
import java.util.List;
import java.util.Optional;

/**
 * Абстрактний інтерфейс для управління станом потоків.
 * Дозволяє легко підмінити Postgres на In-Memory сховище під час прототипування.
 */
public interface JanusStreamRepository {
    JanusLiveStream save(JanusLiveStream stream);
    Optional<JanusLiveStream> findByStationId(String stationId);
    List<JanusLiveStream> findAll();
    void deleteByStationId(String stationId);
    boolean existsByRtpPort(Integer rtpPort);
}
