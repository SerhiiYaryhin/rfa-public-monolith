package media.toloka.rfa.media.webrtc.service.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.media.messanger.model.MessageRoom;
import media.toloka.rfa.media.messanger.repository.MessageRoomRepository;
import media.toloka.rfa.media.webrtc.model.dto.EfirStatusDto;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.radio.repository.StationRepo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 🎛️ Сервіс модерації прямого ефіру.
 * Відповідає за:
 * - Агрегований статус ефіру (кімнати + учасники + аудіо-індикатори).
 * - Модераційні дії: mute / kick (виконуються на боці Janus).
 * - Переміщення учасників між кімнатами (команда клієнту через STOMP).
 * - Публікацію контрольних подій у STOMP-топік /topic/efir.{stationUuid}.control.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EfirModerationService {

    private final StationRepo stationRepo;
    private final MessageRoomRepository messageRoomRepository;
    private final JanusParticipantService participantService;
    private final ModerationService moderationService;
    private final SimpMessagingTemplate messagingTemplate;

    /** Шаблон топіку для контрольних подій панелі модератора та клієнтів. */
    public static final String CONTROL_TOPIC = "/topic/efir.%s.control";

    // ============================================================
    // СТАТУС ЕФІРУ
    // ============================================================

    /**
     * Будує повний стан ефіру для панелі модератора.
     */
    @Transactional(readOnly = true)
    public EfirStatusDto getStatus(String stationUuid) {
        Station station = requireStation(stationUuid);

        EfirStatusDto dto = new EfirStatusDto();
        dto.setStationUuid(station.getUuid());
        dto.setStationName(station.getName());

        // Додаємо всі кімнати станції: головну та три кімнати ефіру
        addRoom(dto, station.getRoomuuid(), "MAIN");
        addRoom(dto, station.getGreenRoomUuid(), "GREEN");
        addRoom(dto, station.getBroadcastRoomUuid(), "BROADCAST");
        addRoom(dto, station.getStaffRoomUuid(), "STAFF");

        // Ефір активний, якщо кімната BROADCAST у стані online
        dto.setEfirActive(dto.getRooms().stream()
                .anyMatch(r -> "BROADCAST".equals(r.getType()) && r.isOnline()));

        return dto;
    }

    private void addRoom(EfirStatusDto dto, String roomUuid, String type) {
        if (roomUuid == null) return;
        MessageRoom room = messageRoomRepository.getByUuid(roomUuid);
        if (room == null) return;

        EfirStatusDto.RoomDto rd = new EfirStatusDto.RoomDto();
        rd.setUuid(room.getUuid());
        rd.setJanusRoomId(room.getId()); // MessageRoom.id = Janus room id
        rd.setName(room.getRoomname());
        rd.setType(type);
        rd.setOnline(Boolean.TRUE.equals(room.getRoomOnlineStatus()));
        rd.setParticipants(listParticipantsSafe(room.getId()));

        dto.getRooms().add(rd);
    }

    /**
     * Безпечне отримання учасників кімнати з Janus.
     */
    private List<EfirStatusDto.ParticipantDto> listParticipantsSafe(Long janusRoomId) {
        List<EfirStatusDto.ParticipantDto> result = new ArrayList<>();
        try {
            List<Map<String, Object>> raw = participantService.listParticipants(janusRoomId);
            if (raw == null) return result;

            for (Map<String, Object> p : raw) {
                EfirStatusDto.ParticipantDto pd = new EfirStatusDto.ParticipantDto();
                pd.setId(toLong(p.get("id")));
                pd.setDisplay(String.valueOf(p.getOrDefault("display", "")));
                pd.setMuted(toBool(p.get("muted")));
                pd.setTalking(toBool(p.get("talking")));   // аудіо-індикатор "хто говорить"
                pd.setSetup(toBool(p.getOrDefault("setup", Boolean.TRUE)));
                result.add(pd);
            }
        } catch (Exception e) {
            log.warn("⚠ Не вдалося отримати учасників кімнати Janus {}: {}", janusRoomId, e.getMessage());
        }
        return result;
    }

    // ============================================================
    // МОДЕРАЦІЙНІ ДІЇ
    // ============================================================

    /**
     * Вимкнути/увімкнути мікрофон учасника (серверна дія в Janus).
     */
    @Transactional(readOnly = true)
    public void mute(String stationUuid, Long janusRoomId, Long participantId, boolean muted) {
        requireRoomBelongsToStation(stationUuid, janusRoomId);
        moderationService.muteParticipant(janusRoomId, participantId, muted);
        log.info("🎛️ Модерація: {} учасника {} у кімнаті {}", muted ? "mute" : "unmute", participantId, janusRoomId);

        publishControl(stationUuid, Map.of(
                "action", muted ? "PARTICIPANT_MUTED" : "PARTICIPANT_UNMUTED",
                "roomId", janusRoomId,
                "participantId", participantId
        ));
    }

    /**
     * Вигнати учасника з кімнати (серверна дія в Janus).
     */
    @Transactional(readOnly = true)
    public void kick(String stationUuid, Long janusRoomId, Long participantId) {
        requireRoomBelongsToStation(stationUuid, janusRoomId);
        moderationService.kickParticipant(janusRoomId, participantId);
        log.info("🎛️ Модерація: kick учасника {} з кімнати {}", participantId, janusRoomId);

        publishControl(stationUuid, Map.of(
                "action", "PARTICIPANT_KICKED",
                "roomId", janusRoomId,
                "participantId", participantId
        ));
    }

    /**
     * Переміщення учасника між кімнатами.
     * Janus AudioBridge не вміє "переносити" WebRTC-клієнт серверно,
     * тому ми надсилаємо команду через STOMP, і КЛІЄНТ сам виконує
     * leave з поточної кімнати та join у цільову.
     */
    @Transactional(readOnly = true)
    public void move(String stationUuid, Long participantId, String targetRoomUuid) {
        Station station = requireStation(stationUuid);

        MessageRoom target = messageRoomRepository.getByUuid(targetRoomUuid);
        if (target == null || !isRoomOfStation(station, target.getId())) {
            throw new IllegalArgumentException("Цільова кімната не належить станції " + stationUuid);
        }

        log.info("🎛️ Модерація: команда MOVE учаснику {} у кімнату {}", participantId, targetRoomUuid);

        publishControl(stationUuid, Map.of(
                "action", "MOVE_PARTICIPANT",
                "participantId", participantId,
                "targetRoomUuid", target.getUuid(),
                "targetJanusRoomId", target.getId(),
                "targetRoomName", target.getRoomname()
        ));
    }

    // ============================================================
    // STOMP ПУБЛІКАЦІЇ
    // ============================================================

    /**
     * Публікує контрольну подію для панелі модератора та всіх клієнтів ефіру.
     * Через RabbitMQ STOMP relay подія автоматично розлітається по всіх вузлах кластера.
     */
    public void publishControl(String stationUuid, Map<String, Object> payload) {
        messagingTemplate.convertAndSend(String.format(CONTROL_TOPIC, stationUuid), payload);
    }

    // ============================================================
    // ДОПОМІЖНІ МЕТОДИ / ВАЛІДАЦІЯ
    // ============================================================

    private Station requireStation(String stationUuid) {
        Station station = stationRepo.getStationByUuid(stationUuid);
        if (station == null) {
            throw new IllegalArgumentException("Станцію з UUID " + stationUuid + " не знайдено");
        }
        return station;
    }

    private void requireRoomBelongsToStation(String stationUuid, Long janusRoomId) {
        Station station = requireStation(stationUuid);
        if (!isRoomOfStation(station, janusRoomId)) {
            throw new IllegalArgumentException(
                    "Кімната Janus " + janusRoomId + " не належить станції " + stationUuid);
        }
    }

    /** Перевіряє, що кімната з таким Janus ID (MessageRoom.id) належить станції. */
    private boolean isRoomOfStation(Station station, Long roomId) {
        if (roomId == null) return false;
        return Stream.of(station.getRoomuuid(), station.getGreenRoomUuid(),
                        station.getBroadcastRoomUuid(), station.getStaffRoomUuid())
                .filter(java.util.Objects::nonNull)
                .map(messageRoomRepository::getByUuid)
                .filter(java.util.Objects::nonNull)
                .anyMatch(r -> roomId.equals(r.getId()));
    }

    private static Long toLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        if (o instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private static boolean toBool(Object o) {
        if (o instanceof Boolean b) return b;
        if (o instanceof String s) return Boolean.parseBoolean(s);
        return false;
    }
}