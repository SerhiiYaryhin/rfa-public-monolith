package media.toloka.rfa.webrtc.service;

import media.toloka.rfa.webrtc.model.JanusLiveStream;
import media.toloka.rfa.webrtc.repository.JanusStreamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PortAllocationService {

    private static final int START_PORT = 10000;
    private static final int END_PORT = 15000;

    private final JanusStreamRepository repository;

    public PortAllocationService(JanusStreamRepository repository) {
        this.repository = repository;
    }

    /**
     * Пошук першого вільного UDP порту в діапазоні, якого немає в базі даних
     */
    @Transactional(readOnly = true)
    public Integer findNextFreePort() {
        for (int port = START_PORT; port <= END_PORT; port++) {
            // Запит у БД: SELECT EXISTS (SELECT 1 FROM janus_live_streams WHERE rtp_port = ?)
            if (!repository.existsByRtpPort(port)) {
                return port;
            }
        }
        throw new RuntimeException("Критична помилка: усі UDP порти в діапазоні 10000-15000 зайняті!");
    }

    @Transactional
    public JanusLiveStream saveActiveStream(String stationId, Long janusRoomId, Integer rtpPort, Integer ffmpegPid) {
        JanusLiveStream stream = new JanusLiveStream(stationId, janusRoomId, rtpPort, ffmpegPid);
        return repository.save(stream); // Тепер повертаємо збережений об'єкт із ID
    }

    @Transactional(readOnly = true)
    public Optional<JanusLiveStream> getStreamByStation(String stationId) {
        return repository.findByStationId(stationId);
    }

    @Transactional
    public void deleteStream(String stationId) {
        repository.deleteByStationId(stationId);
    }
}