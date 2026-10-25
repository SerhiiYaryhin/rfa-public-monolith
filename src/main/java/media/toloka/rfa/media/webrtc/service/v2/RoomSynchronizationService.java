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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 🔄 Сервіс синхронізації кімнат з Janus Gateway.
 *
 * Архітектурні принципи:
 * ─────────────────────
 * 1. ПОВНА СИНХРОНІЗАЦІЯ виконується ТІЛЬКИ при старті сервера
 *    (@EventListener ApplicationReadyEvent). Це єдиний момент, коли
 *    ми видаляємо осиротілі кімнати та відновлюємо втрачені.
 *
 * 2. getActiveRoomsInfo() тепер є READ-ONLY операцією.
 *    Вона НЕ видаляє і НЕ створює кімнати. Тільки збагачує дані
 *    з БД інформацією про учасників з Janus. Це усуває race conditions
 *    та зайве навантаження на Janus Admin API при кожному REST-запиті.
 *
 * 3. Створення службових кімнат (Green/Broadcast/Staff) відбувається
 *    через BroadcastLifecycleService.ensureAllStationRoomsExist(),
 *    який викликається при підготовці ефіру або переході станції в ONLINE.
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

    // ================================================================
    // 🚀 ПОВНА СИНХРОНІЗАЦІЯ ПРИ СТАРТІ СЕРВЕРА
    // ================================================================

    /**
     * Виконується один раз при запуску додатку.
     *
     * Порядок дій:
     *   1. Збирає ВСІ валідні Janus Room IDs з БД (main + green + broadcast + staff)
     *   2. Отримує список кімнат з Janus Gateway
     *   3. Видаляє осиротілі кімнати (є в Janus, але немає в БД)
     *   4. Відновлює втрачені кімнати (є в БД як онлайн, але немає в Janus)
     *
     * ⚠️ Цей метод є ЄДИНИМ місцем, де відбувається видалення кімнат з Janus.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void fullSyncOnStartup() {
        log.info("🚀 === ПОВНА СИНХРОНІЗАЦІЯ КІМНАТ ПРИ СТАРТІ ===");

        try {
            // Крок 1: Збираємо всі валідні ID кімнат з БД
            Set<Long> validRoomIds = collectAllValidRoomIds();
            log.info("📋 Знайдено {} валідних Janus Room IDs у БД", validRoomIds.size());

            if (validRoomIds.isEmpty()) {
                log.info("ℹ️ Немає станцій з кімнатами. Синхронізацію завершено.");
                return;
            }

            // Крок 2: Отримуємо поточний стан Janus
            List<Map<String, Object>> janusRooms;
            try {
                janusRooms = roomService.getJanusRoomsAndParticipants();
                log.info("📡 Отримано {} кімнат з Janus Gateway", janusRooms.size());
            } catch (Exception e) {
                log.error("❌ Не вдалося отримати дані з Janus при старті. " +
                        "Синхронізація пропущена. Кімнати будуть створені при першому запиті.", e);
                return;
            }

            // Будуємо мапу для швидкого пошуку
            Map<Long, Map<String, Object>> janusRoomsMap = new HashMap<>();
            for (Map<String, Object> jRoom : janusRooms) {
                Number roomIdNum = (Number) jRoom.get("room");
                if (roomIdNum != null) {
                    janusRoomsMap.put(roomIdNum.longValue(), jRoom);
                }
            }

            // Крок 3: Видаляємо осиротілі кімнати (є в Janus, немає в БД)
            removeOrphanRooms(janusRoomsMap, validRoomIds);

            // Крок 4: Відновлюємо втрачені кімнати для онлайн-станцій
            recoverMissingRooms(validRoomIds, janusRoomsMap);

            log.info("✅ === ПОВНУ СИНХРОНІЗАЦІЮ ЗАВЕРШЕНО ===");

        } catch (Exception e) {
            log.error("❌ Критична помилка повної синхронізації", e);
        }
    }

    // ================================================================
    // 📖 READ-ONLY: ОТРИМАННЯ СТАНУ ДЛЯ ПАНЕЛІ МОДЕРАТОРА
    // ================================================================

    /**
     * ✅ БЕЗПЕЧНИЙ READ-ONLY метод для панелі модератора.
     *
     * НЕ видаляє і НЕ створює кімнати. Тільки збагачує дані з БД
     * інформацією про учасників з Janus.
     *
     * Якщо Janus недоступний — повертає дані з БД без учасників.
     * Це гарантує, що панель модератора завжди працює, навіть якщо
     * медіасервер тимчасово впав.
     *
     * @return Список станцій з даними про кімнати та учасників
     */
    @Transactional(readOnly = true)
    public List<StationDto> getActiveRoomsInfo() {
        log.debug("📖 getActiveRoomsInfo() — read-only запит стану кімнат");

        // Отримуємо онлайн-станції з БД (це швидко, тільки SQL)
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

        // Намагаємося отримати дані з Janus, але НЕ падаємо якщо не вийшло
        Map<Long, Map<String, Object>> janusRoomsMap = loadJanusRoomsSafe();

        // Збагачуємо кожну станцію даними з Janus
        for (StationDto station : activeStations) {
            Long roomId = station.getId();
            String stationIdStr = String.valueOf(roomId);

            Map<String, Object> jRoomData = janusRoomsMap.get(roomId);
            Optional<JanusLiveStream> activeStream = portService.getStreamByStation(stationIdStr);

            if (jRoomData != null) {
                // Кімната існує в Janus — збагачуємо учасниками
                boolean isLive = activeStream.isPresent();
                enrichStationFromJanus(station, jRoomData, isLive);
            } else {
                // Кімнати немає в Janus — позначаємо як офлайн
                // ⚠️ НЕ створюємо тут! Створення відбувається тільки при старті
                // або через BroadcastLifecycleService.ensureAllStationRoomsExist()
                station.setStationstate(false);
                station.setParticipants(new ArrayList<>());
            }
        }

        return activeStations;
    }

    // ================================================================
    // 🔧 ДОПОМІЖНІ МЕТОДИ
    // ================================================================

    /**
     * Безпечно завантажує кімнати з Janus.
     * При помилці повертає порожню мапу замість виключення.
     */
    private Map<Long, Map<String, Object>> loadJanusRoomsSafe() {
        try {
            List<Map<String, Object>> janusRooms = roomService.getJanusRoomsAndParticipants();
            Map<Long, Map<String, Object>> map = new HashMap<>();
            for (Map<String, Object> jRoom : janusRooms) {
                Number roomIdNum = (Number) jRoom.get("room");
                if (roomIdNum != null) {
                    map.put(roomIdNum.longValue(), jRoom);
                }
            }
            return map;
        } catch (Exception e) {
            log.warn("⚠️ Не вдалося отримати дані з Janus (read-only). " +
                    "Повертаємо дані тільки з БД: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Збирає ВСІ валідні Janus Room IDs з БД.
     * Включає main + green + broadcast + staff для кожної станції,
     * яка має хоча б одну кімнату.
     *
     * @return Set числових ID кімнат (MessageRoom.id = Janus Room ID)
     */
    private Set<Long> collectAllValidRoomIds() {
        Set<Long> validIds = new HashSet<>();

        // Отримуємо всі станції, які мають прив'язану основну кімнату
        List<Station> stations = stationRepo.findByRoomuuidIsNotNull();

        for (Station station : stations) {
            // Додаємо основну кімнату станції
            addRoomIdIfValid(validIds, station.getRoomuuid());

            // Додаємо Green Room (кімната підготовки гостей)
            addRoomIdIfValid(validIds, station.getGreenRoomUuid());

            // Додаємо Broadcast Room (кімната прямого ефіру)
            addRoomIdIfValid(validIds, station.getBroadcastRoomUuid());

            // Додаємо Staff Room (кімната технічного персоналу)
            addRoomIdIfValid(validIds, station.getStaffRoomUuid());
        }

        log.debug("Зібрано {} валідних Janus Room IDs для {} станцій",
                validIds.size(), stations.size());
        return validIds;
    }

    /**
     * Допоміжний метод: знаходить MessageRoom за UUID і додає її
     * числовий ID (= Janus Room ID) у множину валідних ID.
     *
     * @param validIds Множина, куди додається ID
     * @param roomUuid UUID кімнати чату (MessageRoom.uuid)
     */
    private void addRoomIdIfValid(Set<Long> validIds, String roomUuid) {
        if (roomUuid == null || roomUuid.isEmpty()) return;
        try {
            MessageRoom room = messageRoomRepository.getByUuid(roomUuid);
            if (room != null && room.getId() != null) {
                validIds.add(room.getId());
            }
        } catch (Exception e) {
            log.warn("⚠️ Не вдалося знайти MessageRoom для UUID {}: {}",
                    roomUuid, e.getMessage());
        }
    }

    /**
     * Видаляє осиротілі кімнати з Janus Gateway.
     *
     * Осиротіла кімната = кімната, яка існує в пам'яті Janus,
     * але НЕ має відповідного запису в БД (MessageRoom).
     *
     * Перед видаленням евакуює всіх учасників у головну кімнату чату.
     *
     * ⚠️ Викликається ТІЛЬКИ при повній синхронізації на старті сервера.
     *
     * @param janusRoomsMap Мапа кімнат Janus (roomId → дані)
     * @param validRoomIds  Множина валідних ID з БД
     */
    private void removeOrphanRooms(Map<Long, Map<String, Object>> janusRoomsMap,
                                   Set<Long> validRoomIds) {
        int removedCount = 0;

        for (Long janusRoomId : janusRoomsMap.keySet()) {
            if (!validRoomIds.contains(janusRoomId)) {
                log.warn("🗑️ Зайва кімната в Janus: {}. Видаляємо...", janusRoomId);

                Map<String, Object> orphanRoom = janusRoomsMap.get(janusRoomId);
                List<?> participants = (List<?>) orphanRoom.get("participants");

                // Евакуація учасників перед видаленням кімнати
                if (participants != null && !participants.isEmpty()) {
                    log.info("🚨 Евакуація {} користувачів з осиротілої кімнати {}",
                            participants.size(), janusRoomId);
                    try {
                        messangerService.moveUsersToMainChatRoom(janusRoomId);
                    } catch (Exception e) {
                        log.error("❌ Помилка евакуації користувачів з кімнати {}", janusRoomId, e);
                    }
                }

                // Видаляємо кімнату з Janus
                try {
                    roomService.destroyAudioBridgeRoom(janusRoomId);
                    log.info("✅ Кімнату {} успішно видалено з Janus", janusRoomId);
                    removedCount++;
                } catch (Exception e) {
                    log.error("❌ Помилка видалення кімнати {} з Janus", janusRoomId, e);
                }
            }
        }

        if (removedCount > 0) {
            log.info("🧹 Видалено {} осиротілих кімнат з Janus", removedCount);
        } else {
            log.info("✅ Осиротілих кімнат не знайдено. Janus чистий.");
        }
    }

    /**
     * Відновлює кімнати, які є в БД як онлайн, але відсутні в Janus.
     *
     * Це може статися після:
     *   - Перезавантаження Janus Gateway
     *   - Збою мережі між Spring Boot і Janus
     *   - Ручного очищення кімнат через Janus Admin UI
     *
     * Для кожної втраченої кімнати:
     *   1. Створює AudioBridge room у Janus
     *   2. Якщо є активний стрім у БД — відновлює Plain RTP + FFmpeg
     *
     * ⚠️ Викликається ТІЛЬКИ при повній синхронізації на старті сервера.
     */
    private void recoverMissingRooms(Set<Long> validRoomIds,
                                     Map<Long, Map<String, Object>> janusRoomsMap) {
        // Отримуємо онлайн-станції для перевірки
        List<StationDto> onlineStations = messangerService.getOnlineRoom().stream()
                .map(room -> {
                    StationDto dto = new StationDto();
                    dto.setId(room.getId());
                    dto.setRoomuuid(room.getUuid());
                    dto.setRoomname(room.getRoomname());
                    dto.setRoomOnlineStatus(room.getRoomOnlineStatus());
                    return dto;
                })
                .collect(Collectors.toList());

        int recoveredCount = 0;

        for (StationDto station : onlineStations) {
            Long roomId = station.getId();

            // Перевіряємо тільки ті кімнати, які є в validRoomIds
            // (тобто мають запис у БД), але відсутні в Janus
            if (validRoomIds.contains(roomId) && !janusRoomsMap.containsKey(roomId)) {
                String stationIdStr = String.valueOf(roomId);
                Optional<JanusLiveStream> activeStream = portService.getStreamByStation(stationIdStr);

                log.info("🔧 Кімната {} ({}) відсутня в Janus. Відновлюємо...",
                        roomId, station.getRoomname());

                recoverLostRoom(station, stationIdStr, roomId, activeStream);
                recoveredCount++;
            }
        }

        if (recoveredCount > 0) {
            log.info("🔧 Відновлено {} втрачених кімнат у Janus", recoveredCount);
        } else {
            log.info("✅ Всі онлайн-кімнати присутні в Janus. Відновлення не потрібне.");
        }
    }

    /**
     * Збагачує StationDto даними з Janus (учасники, статус LIVE).
     *
     * @param station   DTO станції для оновлення
     * @param jRoomData Дані кімнати з Janus (включаючи participants)
     * @param isLive    true якщо є активний стрім у БД (LIVE індикатор)
     */
    private void enrichStationFromJanus(StationDto station,
                                        Map<String, Object> jRoomData,
                                        boolean isLive) {
        // Станція вважається LIVE тільки якщо:
        //   1. Кімната існує в Janus
        //   2. Є активний запис про стрім у БД (JanusLiveStream)
        station.setStationstate(isLive);

        List<Object> participants = (List<Object>) jRoomData.get("participants");
        station.setParticipants(participants != null ? participants : new ArrayList<>());
    }

    /**
     * Відновлює одну втрачену кімнату в Janus.
     * Створює AudioBridge room і, якщо є активний стрім,
     * відновлює Plain RTP учасника та перезапускає FFmpeg.
     */
    private void recoverLostRoom(StationDto station, String stationIdStr,
                                 Long roomId, Optional<JanusLiveStream> activeStream) {
        try {
            // Створюємо кімнату в Janus
            roomService.createAudioBridgeRoom(roomId, station.getRoomname());
            log.info("✅ Кімнату {} створено в Janus", roomId);

            // Якщо є активний стрім — відновлюємо Plain RTP
            if (activeStream.isPresent()) {
                recoverPlainRtpParticipant(stationIdStr, roomId, activeStream.get());
            }

        } catch (Exception e) {
            log.error("❌ Помилка відновлення кімнати {} в Janus: {}", roomId, e.getMessage(), e);
        }
    }

    /**
     * Відновлює Plain RTP учасника для трансляції радіо в кімнату Janus.
     *
     * Процес:
     *   1. Створює нову сесію та хендл плагіна в Janus
     *   2. Приєднується як RTP-учасник до кімнати
     *   3. Отримує порт Janus для прийому RTP
     *   4. Зберігає сесію/хендл у БД
     *   5. Перезапускає FFmpeg на новий порт Janus
     */
    private void recoverPlainRtpParticipant(String stationIdStr, Long roomId,
                                            JanusLiveStream stream) {
        log.info("📻 Відновлення Plain RTP для станції {} у кімнаті {}", stationIdStr, roomId);

        try {
            // Створюємо персистентну сесію в Janus
            Long sessionId = sessionManager.createSession();
            Long handleId = sessionManager.attachPlugin(sessionId, "janus.plugin.audiobridge");

            // Приєднуємося як RTP-учасник
            Map<String, Object> joinResponse = participantService.joinAsRtpParticipant(
                    sessionId, handleId, roomId,
                    "Радіо Ефір", ffmpegAgentHost, stream.getRtpPort()
            );

            // Парсимо відповідь для отримання порту Janus
            Map<String, Object> plugindata = (Map<String, Object>) joinResponse.get("plugindata");
            Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
            Map<String, Object> rtp = (Map<String, Object>) data.get("rtp");
            Integer janusRtpPort = ((Number) rtp.get("port")).intValue();

            // Оновлюємо сесію/хендл у БД
            stream.setJanusSessionId(sessionId);
            stream.setJanusHandleId(handleId);
            portService.saveStream(stream);

            // Перезапускаємо FFmpeg на новий порт Janus
            ffmpegService.sendStopCommand(stationIdStr);
            String rtpUrl = String.format(
                    "rtp://janus.rfa:%d?localport=%d&pkt_size=1200",
                    janusRtpPort, stream.getRtpPort()
            );
            String sourceStreamUrl = "http://icecast.rfa:8000/ukraine";
            ffmpegService.sendStartCommand(stationIdStr, sourceStreamUrl, rtpUrl);

            log.info("✅ Plain RTP відновлено для кімнати {}: session={}, handle={}, janusPort={}",
                    roomId, sessionId, handleId, janusRtpPort);

        } catch (Exception ex) {
            log.error("❌ Помилка відновлення Plain RTP для кімнати {}: {}",
                    roomId, ex.getMessage(), ex);
        }
    }
}