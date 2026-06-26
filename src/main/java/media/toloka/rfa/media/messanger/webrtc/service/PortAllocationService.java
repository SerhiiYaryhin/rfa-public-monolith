package media.toloka.rfa.media.messanger.webrtc.service;

import media.toloka.rfa.media.messanger.webrtc.model.JanusLiveStream;
import media.toloka.rfa.media.messanger.webrtc.repository.JanusStreamRepository;
import media.toloka.rfa.media.messanger.webrtc.service.v2.JanusRabbitClientOptimized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Сервіс для динамічного виділення UDP-портів для RTP-трансляцій.
 * 
 * Забезпечує унікальність портів у діапазоні 10000-15000, перевіряючи 
 * стан активних трансляцій у базі даних. Це критично для запобігання 
 * конфліктам при одночасному запуску декількох ефірів.
 */
@Service
public class PortAllocationService {

    // Початковий та кінцевий порти згідно з конфігурацією rtp_port_range у janus.jcfg
    // todo винести порти в propertyes

    private static final int START_PORT = 49152;
    private static final int END_PORT = 49999;

    private final JanusStreamRepository streamRepository;
    private static final Logger log = LoggerFactory.getLogger(PortAllocationService.class);

    public PortAllocationService(JanusStreamRepository repository) {
        this.streamRepository = repository;
    }

    /**
     * Пошук наступного вільного UDP порту.
     * Проходить по всьому діапазону та шукає перший порт, який наразі не використовується.
     */
    @Transactional(readOnly = true)
    public Integer findNextFreePort() {
        for (int port = START_PORT; port <= END_PORT; port++) {
            // Перевірка в БД: SELECT EXISTS (SELECT 1 FROM janus_live_streams WHERE rtp_port = ?)
            if (!streamRepository.existsByRtpPort(port)) {
                return port;
            }
        }
        throw new RuntimeException("Усі UDP порти в діапазоні 10000-15000 зайняті. Неможливо запустити новий ефір.");
    }

    /**
     * Збереження інформації про активний стрім у БД.
     */
    @Transactional
    public JanusLiveStream saveActiveStream(String stationId, Long janusRoomId, Integer rtpPort, Integer ffmpegPid) {
        JanusLiveStream stream = new JanusLiveStream(stationId, janusRoomId, rtpPort, ffmpegPid);
        return streamRepository.save(stream);
    }

    /**
     * Отримання стріму за ідентифікатором радіостанції.
     */
    @Transactional(readOnly = true)
    public Optional<JanusLiveStream> getStreamByStation(String stationId) {
        return streamRepository.findByStationId(stationId);
    }

    /**
     * Отримання з бази стріму за ID кімнати Janus.
     * todo спробувати отримати кімнати з janus
     */
    @Transactional(readOnly = true)
    public Optional<JanusLiveStream> getStreamByJanusRoomId(Long janusRoomId) {
        return streamRepository.findByJanusRoomId(janusRoomId);
    }

    /**
     * Видалення стріму з бази (звільнення порту).
     */
    @Transactional
    public void deleteStream(String stationId) {
        streamRepository.deleteByStationId(stationId);
    }

    /**
     * Отримання списку всіх активних стрімів.
     */
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
