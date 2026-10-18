package media.toloka.rfa.media.webrtc.service.v2;

import media.toloka.rfa.media.messanger.service.MessangerService;
import media.toloka.rfa.media.webrtc.model.JanusLiveStream;
import media.toloka.rfa.media.webrtc.model.dto.StationDto;
import media.toloka.rfa.media.webrtc.service.PortAllocationService;
import media.toloka.rfa.radio.station.service.StationService; // ✅ ДОДАНО ЦЕЙ ІМПОРТ
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoomSynchronizationService {

    private static final Logger log = LoggerFactory.getLogger(RoomSynchronizationService.class);

    private final JanusRoomService roomService;
    private final JanusParticipantService participantService;
    private final JanusSessionManager sessionManager;
    private final PortAllocationService portService;
    private final FfmpegCommandService ffmpegService;
    private final MessangerService messangerService;
    private final StationService stationService;

    public RoomSynchronizationService(
            JanusRoomService roomService,
            JanusParticipantService participantService,
            JanusSessionManager sessionManager,
            PortAllocationService portService,
            FfmpegCommandService ffmpegService,
            MessangerService messangerService,
            StationService stationService) {
        this.roomService = roomService;
        this.participantService = participantService;
        this.sessionManager = sessionManager;
        this.portService = portService;
        this.ffmpegService = ffmpegService;
        this.messangerService = messangerService;
        this.stationService = stationService;
    }

    @Transactional
    public List<StationDto> getActiveRoomsInfo() {
        log.debug("Отримання списку активних кімнат");

        List<StationDto> activeStations = stationService.getActiveStations();
        Set<Long> validRoomIds = activeStations.stream().map(StationDto::getId).collect(Collectors.toSet());

        List<Map<String, Object>> janusRooms;
        try {
            janusRooms = roomService.getJanusRoomsAndParticipants();
            log.debug("Отримано {} кімнат з Janus", janusRooms.size());
        } catch (Exception e) {
            log.error("Помилка отримання даних з Janus, повертаємо дані з БД", e);
            return activeStations;
        }

        Map<Long, Map<String, Object>> janusRoomsMap = new HashMap<>();
        for (Map<String, Object> jRoom : janusRooms) {
            Number roomIdNum = (Number) jRoom.get("room");
            if (roomIdNum != null) {
                janusRoomsMap.put(roomIdNum.longValue(), jRoom);
            }
        }

        removeOrphanRooms(janusRoomsMap, validRoomIds);

        for (StationDto station : activeStations) {
            Long roomId = station.getId();
            String stationIdStr = String.valueOf(roomId);
            Map<String, Object> jRoomData = janusRoomsMap.get(roomId);

            if (jRoomData != null) {
                enrichStationFromJanus(station, jRoomData);
            } else {
                recoverLostRoom(station, stationIdStr, roomId);
            }
        }

        return activeStations;
    }

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

    private void enrichStationFromJanus(StationDto station, Map<String, Object> jRoomData) {
        station.setStationstate(true);
        List<Object> participants = (List<Object>) jRoomData.get("participants");
        station.setParticipants(participants != null ? participants : new ArrayList<>());
    }

    private void recoverLostRoom(StationDto station, String stationIdStr, Long roomId) {
        log.info("Кімната {} відсутня в Janus. Створюємо...", roomId);
        try {
            roomService.createAudioBridgeRoom(roomId, station.getName());

            Optional<JanusLiveStream> existingStream = portService.getStreamByStation(stationIdStr);
            if (existingStream.isPresent()) {
                recoverPlainRtpParticipant(stationIdStr, roomId, existingStream.get());
            }

            station.setStationstate(true);
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
                    sessionId, handleId, roomId, "Радіо Ефір", "127.0.0.1", stream.getRtpPort()
            );

            Map<String, Object> plugindata = (Map<String, Object>) joinResponse.get("plugindata");
            Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
            Map<String, Object> rtp = (Map<String, Object>) data.get("rtp");
            Integer janusRtpPort = ((Number) rtp.get("port")).intValue();

            stream.setJanusSessionId(sessionId);
            stream.setJanusHandleId(handleId);
            portService.saveStream(stream);

            ffmpegService.sendStopCommand(stationIdStr);
            String rtpUrl = String.format("rtp://janus.rfa:%d?localport=%d&pkt_size=1200", janusRtpPort, stream.getRtpPort());
            String sourceStreamUrl = "http://icecast.rfa:8000/ukraine";
            ffmpegService.sendStartCommand(stationIdStr, sourceStreamUrl, rtpUrl);

            log.info("Plain RTP успішно відновлено для кімнати {}: session={}, handle={}, port={}", roomId, sessionId, handleId, janusRtpPort);
        } catch (Exception ex) {
            log.error("Помилка відновлення Plain RTP для кімнати {}: {}", roomId, ex.getMessage(), ex);
        }
    }
}