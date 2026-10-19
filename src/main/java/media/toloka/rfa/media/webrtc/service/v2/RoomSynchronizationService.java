package media.toloka.rfa.media.webrtc.service.v2;

import media.toloka.rfa.media.messanger.service.MessangerService;
import media.toloka.rfa.media.webrtc.model.JanusLiveStream;
import media.toloka.rfa.media.webrtc.model.dto.StationDto;
import media.toloka.rfa.media.webrtc.service.PortAllocationService;
import media.toloka.rfa.radio.station.service.StationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 🔄 Сервіс синхронізації кімнат з Janus Gateway.
 * Відповідає за отримання списку кімнат, видалення осиротілих кімнат та самовідновлення.
 *
 * Логіка визначення стану трансляції (LIVE індикатор):
 * - LIVE (stationstate=true): кімната є в Janus І є активний запис про стрім в БД
 * - НЕ LIVE (stationstate=false): кімнати немає в Janus, або немає запису в БД
 */
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

    /**
     * Отримання списку активних кімнат з синхронізацією та самовідновленням.
     *
     * Для кожної станції перевіряється:
     * 1. Чи існує кімната в Janus Gateway
     * 2. Чи є активний запис про стрім в БД (portService)
     * 3. Якщо кімнати немає в Janus — запускається самовідновлення
     */
    @Transactional
    public List<StationDto> getActiveRoomsInfo() {
        log.debug("Отримання списку активних кімнат");

        List<StationDto> activeStations = stationService.getActiveStations();
        Set<Long> validRoomIds = activeStations.stream()
                .map(StationDto::getId)
                .collect(Collectors.toSet());

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

            // Перевіряємо, чи є активний запис про стрім в БД
            Optional<JanusLiveStream> activeStream = portService.getStreamByStation(stationIdStr);

            if (jRoomData != null) {
                // Кімната існує в Janus
                if (activeStream.isPresent()) {
                    // ✅ Кімната є в Janus І є активний стрім в БД → LIVE
                    enrichStationFromJanus(station, jRoomData, true);
                } else {
                    // ⚠️ Кімната є в Janus, але стрім не активний в БД → НЕ LIVE
                    // (кімната може існувати для ручного підключення користувачів)
                    enrichStationFromJanus(station, jRoomData, false);
                }
            } else {
                // ❌ Кімнати немає в Janus → самовідновлення
                recoverLostRoom(station, stationIdStr, roomId, activeStream);
            }
        }

        return activeStations;
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

    /**
     * Збагачення даних станції інформацією з Janus.
     *
     * @param station Станція для оновлення
     * @param jRoomData Дані кімнати з Janus
     * @param isLive true якщо є активний стрім в БД (показувати LIVE індикатор)
     */
    private void enrichStationFromJanus(StationDto station, Map<String, Object> jRoomData, boolean isLive) {
        station.setStationstate(isLive);
        List<Object> participants = (List<Object>) jRoomData.get("participants");
        station.setParticipants(participants != null ? participants : new ArrayList<>());
    }

    /**
     * Самовідновлення кімнати, яка пропала в Janus Gateway.
     * Якщо в БД є активний стрім — відновлює Plain RTP учасника та перезапускає FFmpeg.
     */
    private void recoverLostRoom(StationDto station, String stationIdStr, Long roomId,
                                 Optional<JanusLiveStream> activeStream) {
        log.info("Кімната {} відсутня в Janus. Створюємо...", roomId);

        try {
            // Створюємо кімнату заново
            roomService.createAudioBridgeRoom(roomId, station.getName());

            if (activeStream.isPresent()) {
                // Відновлюємо Plain RTP учасника та FFmpeg
                recoverPlainRtpParticipant(stationIdStr, roomId, activeStream.get());
            }

            // LIVE тільки якщо є активний стрім в БД
            station.setStationstate(activeStream.isPresent());
            station.setParticipants(new ArrayList<>());
        } catch (Exception e) {
            log.error("Помилка створення кімнати {}", roomId, e);
            station.setStationstate(false);
        }
    }

    /**
     * Відновлення Plain RTP учасника для кімнати після її зникнення в Janus.
     * Перезапускає FFmpeg на новий порт, який виділив Janus.
     */
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

            // Оновлюємо сесії в БД
            stream.setJanusSessionId(sessionId);
            stream.setJanusHandleId(handleId);
            portService.saveStream(stream);

            // Перезапускаємо FFmpeg на новий порт Janus
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