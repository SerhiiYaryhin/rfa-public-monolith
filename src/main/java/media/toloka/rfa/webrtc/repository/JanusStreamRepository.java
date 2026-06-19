package media.toloka.rfa.webrtc.repository;

import media.toloka.rfa.webrtc.model.JanusLiveStream;
import java.util.List;
import java.util.Optional;

/**
 * Рівень абстракції (Repository) для роботи з даними WebRTC стрімів.
 * 
 * Цей інтерфейс визначає методи пошуку, збереження та видалення активних трансляцій.
 * Використання окремого інтерфейсу дозволяє легко змінити реалізацію 
 * (наприклад, з JPA на Redis для більшої швидкодії) без зміни бізнес-логіки.
 */
public interface JanusStreamRepository {
    
    /** Збереження або оновлення стріму */
    JanusLiveStream save(JanusLiveStream stream);
    
    /** Пошук стріму за ідентифікатором станції */
    Optional<JanusLiveStream> findByStationId(String stationId);
    
    /** Пошук стріму за ID кімнати Janus */
    Optional<JanusLiveStream> findByJanusRoomId(Long janusRoomId);
    
    /** Список усіх активних стрімів */
    List<JanusLiveStream> findAll();
    
    /** Видалення інформації про стрім (звільнення ресурсів) */
    void deleteByStationId(String stationId);
    
    /** Перевірка, чи не зайнятий вже UDP порт іншою станцією */
    boolean existsByRtpPort(Integer rtpPort);
}
