package media.toloka.rfa.media.webrtc.service;

import media.toloka.rfa.media.webrtc.model.JanusLiveStream;
import media.toloka.rfa.media.webrtc.repository.JanusStreamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Сервіс для динамічного виділення UDP-портів для RTP-трансляцій.
 */
@Service
public class PortAllocationService {

    private static final int START_PORT = 49000;
//    private static final int START_PORT = 49152;
    private static final int END_PORT = 49150;
//    private static final int END_PORT = 49999;

    private final JanusStreamRepository streamRepository;
    private static final Logger log = LoggerFactory.getLogger(PortAllocationService.class);

    public PortAllocationService(JanusStreamRepository repository) {
        this.streamRepository = repository;
    }

    @Transactional(readOnly = true)
    public Integer findNextFreePort() {
        for (int port = START_PORT; port <= END_PORT; port++) {
            if (!streamRepository.existsByRtpPort(port)) {
                return port;
            }
        }
        throw new RuntimeException("Усі UDP порти в діапазоні 49152-49999 зайняті.");
    }

    /**
     * ✅ НОВЕ: Перевантажений метод для зворотної сумісності з v1 кодом.
     * Викликається з RadioLiveService.java (v1), де rtpForwardStreamId не використовується.
     */
    @Transactional
    public JanusLiveStream saveActiveStream(String stationId, Long janusRoomId, Integer rtpPort, Integer ffmpegPid) {
        return saveActiveStream(stationId, janusRoomId, rtpPort, ffmpegPid, null);
    }

    /**
     * Збереження інформації про активний стрім у БД з rtpForwardStreamId.
     */
    @Transactional
    public JanusLiveStream saveActiveStream(String stationId, Long janusRoomId, Integer rtpPort, Integer ffmpegPid, Long rtpForwardStreamId) {
        JanusLiveStream stream = new JanusLiveStream(stationId, janusRoomId, rtpPort, ffmpegPid);
        stream.setRtpForwardStreamId(rtpForwardStreamId);
        return streamRepository.save(stream);
    }

    @Transactional
    public void updateRtpForwardStreamId(String stationId, Long rtpForwardStreamId) {
        Optional<JanusLiveStream> streamOpt = getStreamByStation(stationId);
        if (streamOpt.isPresent()) {
            JanusLiveStream stream = streamOpt.get();
            stream.setRtpForwardStreamId(rtpForwardStreamId);
            streamRepository.save(stream);
            log.info("Оновлено rtpForwardStreamId для станції {}: {}", stationId, rtpForwardStreamId);
        } else {
            log.warn("Стрім для станції {} не знайдено", stationId);
        }
    }

    @Transactional(readOnly = true)
    public Optional<JanusLiveStream> getStreamByStation(String stationId) {
        return streamRepository.findByStationId(stationId);
    }

    @Transactional(readOnly = true)
    public Optional<JanusLiveStream> getStreamByJanusRoomId(Long janusRoomId) {
        return streamRepository.findByJanusRoomId(janusRoomId);
    }

    @Transactional
    public void deleteStream(String stationId) {
        streamRepository.deleteByStationId(stationId);
    }

    @Transactional(readOnly = true)
    public List<JanusLiveStream> getAllActiveStreams() {
        return streamRepository.findAll();
    }

    @Transactional
    public void updateStreamPort(String stationId, Integer newPort) {
        Optional<JanusLiveStream> streamOpt = getStreamByStation(stationId);
        if (streamOpt.isPresent()) {
            JanusLiveStream stream = streamOpt.get();
            stream.setRtpPort(newPort);
            streamRepository.save(stream);
            log.info("Оновлено порт для станції {}: {}", stationId, newPort);
        } else {
            log.warn("Стрім для станції {} не знайдено", stationId);
        }
    }
}