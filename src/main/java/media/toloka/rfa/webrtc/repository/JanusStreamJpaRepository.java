package media.toloka.rfa.webrtc.repository;

import media.toloka.rfa.webrtc.model.JanusLiveStream;
import media.toloka.rfa.webrtc.repository.JanusStreamRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JanusStreamJpaRepository extends JpaRepository<JanusLiveStream, Long>, JanusStreamRepository {

    @Override
    Optional<JanusLiveStream> findByStationId(String stationId);

    @Override
    void deleteByStationId(String stationId);

    @Override
    boolean existsByRtpPort(Integer rtpPort);
}
