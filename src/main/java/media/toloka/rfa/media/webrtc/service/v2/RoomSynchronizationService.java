package media.toloka.rfa.media.webrtc.service.v2;

import media.toloka.rfa.media.messanger.model.MessageRoom;
import media.toloka.rfa.media.messanger.repository.MessageRoomRepository;
import media.toloka.rfa.media.messanger.service.MessangerService;
import media.toloka.rfa.media.webrtc.model.JanusLiveStream;
import media.toloka.rfa.media.webrtc.model.dto.StationDto;
import media.toloka.rfa.media.webrtc.service.PortAllocationService;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.radio.repository.StationRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 🔄 Сервіс синхронізації кімнат з Janus Gateway.
 Відповідає за отримання списку кімнат, видалення осиротілих кімнат та самовідновлення.

 ✅ ВИПРАВЛЕНО: validRoomIds тепер включає ВСІ кімнати станції
 (main, green, broadcast, staff), а не тільки основну.
 */
@Service
public class RoomSynchronizationService {

    private static final Logger log = LoggerFactory.getLogger(RoomSynchronizationService.class);

    @Value("${ffmpeg.agent.host}")
    private String ffmpegAgentHost;

    private final JanusRoomService roomService;
    private final JanusParticipantService participantService;
    private final JanusSessionManager sessionManager;
    private final PortAllocationService portService;
    private final FfmpegCommandService ffmpegService;
    private final MessangerService messangerService;
    private final StationRepo stationRepo;
    private final MessageRoomRepository messageRoomRepository;

    public RoomSynchronizationService(
            JanusRoomService roomService,
            JanusParticipantService participantService,
            JanusSessionManager sessionManager,
            PortAllocationService portService,
            FfmpegCommandService ffmpegService,
            MessangerService messangerService,
            StationRepo stationRepo,
            MessageRoomRepository messageRoomRepository) {
        this.roomService = roomService;
        this.participantService = participantService;
        this.sessionManager = sessionManager;
        this.portService = portService;
        this.ffmpegService = ffmpegService;
        this.messangerService = messangerService;
        this.stationRepo = stationRepo;
        this.messageRoomRepository = messageRoomRepository;
    }

    /**
     * Отримання списку активних кімнат з синхронізацією та самовідновленням.
     */
    @Transactional
    public List<StationDto> getActiveRoomsInfo() {
        log.debug("Отримання списку активних кімнат");

        List<StationDto> activeStations = messangerService.getOnlineRoom().stream()
                .map(room -> {
                    StationDto dto = new StationDto();
                    dto.setId(room.getId());
                    dto.setRoomuuid(room.getUuid());
                    dto.setRoomname(room.getRoomname());
                    dto.setRoomOnlineStatus(room.getRoomOnlineStatus());
                    return dto;
                })
                .collect(Collectors.toList());

        // ✅ КЛЮЧОВА ЗМІНА: Збираємо ВСІ валідні Janus Room IDs
        // включаючи main, green, broadcast, staff для кожної станції
        Set<Long> validRoomIds = collectAllValidRoomIds();
        log.debug("Валідні Janus Room IDs: {}", validRoomIds);

        // Отримання кімнат з Janus
        List<Map<String, Object>> janusRooms;
        try {
            janusRooms = roomService.getJanusRoomsAndParticipants();
            log.debug("Отримано {} кімнат з Janus", janusRooms.size());
        } catch (Exception e) {
            log.error("Помилка отримання даних з Janus, повертаємо дані з БД", e);
            return activeStations;
        }

        // Мапа кімнат за ID для швидкого пошуку
        Map<Long, Map<String, Object>> janusRoomsMap = new HashMap<>();
        for (Map<String, Object> jRoom : janusRooms) {
            Number roomIdNum = (Number) jRoom.get("room");
            if (roomIdNum != null) {
                janusRoomsMap.put(roomIdNum.longValue(), jRoom);
            }
        }

        // Видалення зайвих (осиротілих) кімнат з Janus
        removeOrphanRooms(janusRoomsMap, validRoomIds);

        // Синхронізація та самовідновлення для кожної станції
        for (StationDto station : activeStations) {
            Long roomId = station.getId();
            String stationIdStr = String.valueOf(roomId);
            Map<String, Object> jRoomData = janusRoomsMap.get(roomId);

            Optional<JanusLiveStream> activeStream = portService.getStreamByStation(stationIdStr);

            if (jRoomData != null) {
                if (activeStream.isPresent()) {
                    enrichStationFromJanus(station, jRoomData, true);
                } else {
                    enrichStationFromJanus(station, jRoomData, false);
                }
            } else {
                recoverLostRoom(station, stationIdStr, roomId, activeStream);
            }
        }

        return activeStations;
    }

    /**
     * ✅ НОВИЙ МЕТОД: Збирає ВСІ валідні Janus Room IDs.
     * Включає основну кімнату + green + broadcast + staff для кожної онлайн-станції.
     */
    private Set<Long> collectAllValidRoomIds() {
        Set<Long> validIds = new HashSet<>();

        // Отримуємо всі станції з кімнатами
        List<Station> stations = stationRepo.findByRoomuuidIsNotNull();

        for (Station station : stations) {
            // Додаємо основну кімнату
            addRoomIdIfValid(validIds, station.getRoomuuid());

            // ✅ Додаємо Green Room
            addRoomIdIfValid(validIds, station.getGreenRoomUuid());

            // ✅ Додаємо Broadcast Room
            addRoomIdIfValid(validIds, station.getBroadcastRoomUuid());

            // ✅ Додаємо Staff Room
            addRoomIdIfValid(validIds, station.getStaffRoomUuid());
        }

        log.info("Зібрано {} валідних Janus Room IDs (включаючи green/broadcast/staff)", validIds.size());
        return validIds;
    }

    /**
     * Допоміжний метод: додає Janus Room ID за UUID кімнати, якщо вона існує.
     */
    private void addRoomIdIfValid(Set<Long> validIds, String roomUuid) {
        if (roomUuid == null || roomUuid.isEmpty()) return;
        try {
            MessageRoom room = messageRoomRepository.getByUuid(roomUuid);
            if (room != null && room.getId() != null) {
                validIds.add(room.getId());
            }
        } catch (Exception e) {
            log.warn("Не вдалося знайти MessageRoom для UUID {}: {}", roomUuid, e.getMessage());
        }
    }

    /**
     * Видалення осиротілих кімнат з Janus (яких немає в БД станцій).
     */
    private void removeOrphanRooms(Map<Long, Map<String, Object>> janusRoomsMap, Set<Long> validRoomIds) {
        for (Long janusRoomId : janusRoomsMap.keySet()) {
            if (!validRoomIds.contains(janusRoomId)) {
                log.warn("Зайва кімната в Janus: {}. Видаляємо...", janusRoomId);

                Map<String, Object> orphanRoom = janusRoomsMap.get(janusRoomId);
                List<?> participants = (List<?>) orphanRoom.get("participants");

                if (participants != null && !participants.isEmpty()) {
                    log.info("Евакуація {} користувачів з кімнати {}", participants.size(), janusRoomId);
                    try {
                        messangerService.moveUsersToMainChatRoom(janusRoomId);
                    } catch (Exception e) {
                        log.error("Помилка евакуації користувачів", e);
                    }
                }

                try {
                    roomService.destroyAudioBridgeRoom(janusRoomId);
                    log.info("Кімнату {} видалено з Janus", janusRoomId);
                } catch (Exception e) {
                    log.error("Помилка видалення кімнати {}", janusRoomId, e);
                }
            }
        }
    }

    private void enrichStationFromJanus(StationDto station, Map<String, Object> jRoomData, boolean isLive) {
        station.setStationstate(isLive);
        List<Object> participants = (List<Object>) jRoomData.get("participants");
        station.setParticipants(participants != null ? participants : new ArrayList<>());
    }

    private void recoverLostRoom(StationDto station, String stationIdStr, Long roomId,
                                 Optional<JanusLiveStream> activeStream) {
        log.info("Кімната {} відсутня в Janus. Створюємо...", roomId);
        try {
            roomService.createAudioBridgeRoom(roomId, station.getRoomname());

            if (activeStream.isPresent()) {
                recoverPlainRtpParticipant(stationIdStr, roomId, activeStream.get());
            }

            station.setStationstate(activeStream.isPresent());
            station.setParticipants(new ArrayList<>());
        } catch (Exception e) {
            log.error("Помилка створення кімнати {}", roomId, e);
            station.setStationstate(false);
        }
    }

    private void recoverPlainRtpParticipant(String stationIdStr, Long roomId, JanusLiveStream stream) {
        log.info("Відновлюємо Plain RTP для станції {}", stationIdStr);
        try {
            Long sessionId = sessionManager.createSession();
            Long handleId = sessionManager.attachPlugin(sessionId, "janus.plugin.audiobridge");

            Map<String, Object> joinResponse = participantService.joinAsRtpParticipant(
                    sessionId, handleId, roomId, "Радіо Ефір", ffmpegAgentHost, stream.getRtpPort()
            );

            Map<String, Object> plugindata = (Map<String, Object>) joinResponse.get("plugindata");
            Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
            Map<String, Object> rtp = (Map<String, Object>) data.get("rtp");
            Integer janusRtpPort = ((Number) rtp.get("port")).intValue();

            stream.setJanusSessionId(sessionId);
            stream.setJanusHandleId(handleId);
            portService.saveStream(stream);

            ffmpegService.sendStopCommand(stationIdStr);
            String rtpUrl = String.format("rtp://janus.rfa:%d?localport=%d&pkt_size=1200",
                    janusRtpPort, stream.getRtpPort());
            String sourceStreamUrl = "http://icecast.rfa:8000/ukraine";
            ffmpegService.sendStartCommand(stationIdStr, sourceStreamUrl, rtpUrl);

            log.info("Plain RTP успішно відновлено для кімнати {}: session={}, handle={}, port={}",
                    roomId, sessionId, handleId, janusRtpPort);
        } catch (Exception ex) {
            log.error("Помилка відновлення Plain RTP для кімнати {}: {}", roomId, ex.getMessage(), ex);
        }
    }
}