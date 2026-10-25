package media.toloka.rfa.media.webrtc.service.v2;

import media.toloka.rfa.media.messanger.model.MessageRoom;
import media.toloka.rfa.media.messanger.repository.MessageRoomRepository;
import media.toloka.rfa.media.webrtc.model.JanusLiveStream;
import media.toloka.rfa.media.webrtc.model.dto.StationDto;
import media.toloka.rfa.media.webrtc.service.PortAllocationService;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.radio.repository.StationRepo;
import media.toloka.rfa.radio.station.service.StationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 🎬 Сервіс життєвого циклу трансляцій та службових кімнат.
 *
 * Відповідає за:
 *   1. Ініціалізацію трансляцій при старті сервера (делегує RoomSynchronizationService)
 *   2. Запуск/зупинку трансляції радіостанції в Janus AudioBridge
 *   3. ✅ НОВЕ: Створення ВСІХ службових кімнат станції (Green/Broadcast/Staff)
 *      при підготовці ефіру або переході станції в ONLINE
 *
 * Архітектурна примітка:
 *   Повна синхронізація (видалення осиротілих кімнат) виконується
 *   через RoomSynchronizationService.fullSyncOnStartup().
 *   Цей сервіс фокусується на створенні та управлінні трансляціями.
 */
@Service
public class BroadcastLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(BroadcastLifecycleService.class);

    @Value("${ffmpeg.agent.host}")
    private String ffmpegAgentHost;

    private final JanusRoomService roomService;
    private final JanusParticipantService participantService;
    private final JanusSessionManager sessionManager;
    private final PortAllocationService portService;
    private final FfmpegCommandService ffmpegService;
    private final StationService stationService;
    private final StationRepo stationRepo;
    private final MessageRoomRepository messageRoomRepository;

    public BroadcastLifecycleService(
            JanusRoomService roomService,
            JanusParticipantService participantService,
            JanusSessionManager sessionManager,
            PortAllocationService portService,
            FfmpegCommandService ffmpegService,
            StationService stationService,
            StationRepo stationRepo,
            MessageRoomRepository messageRoomRepository) {
        this.roomService = roomService;
        this.participantService = participantService;
        this.sessionManager = sessionManager;
        this.portService = portService;
        this.ffmpegService = ffmpegService;
        this.stationService = stationService;
        this.stationRepo = stationRepo;
        this.messageRoomRepository = messageRoomRepository;
    }

    // ================================================================
    // 🚀 ІНІЦІАЛІЗАЦІЯ ПРИ СТАРТІ СЕРВЕРА
    // ================================================================

    /**
     * Ініціалізує трансляції для всіх онлайн-станцій при старті сервера.
     *
     * Логіка:
     *   - Якщо станція онлайн і НЕ має активного стріму → запускаємо трансляцію
     *   - Якщо станція онлайн і ВЖЕ має стрім → пропускаємо (вже працює)
     *
     * ⚠️ Синхронізація кімнат (видалення зайвих, відновлення втрачених)
     *    виконується окремо через RoomSynchronizationService.fullSyncOnStartup()
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeBroadcasts() {
        log.info("🚀 Ініціалізація трансляцій при старті сервера...");

        try {
            List<StationDto> onlineStations = stationService.getActiveStations();

            if (onlineStations.isEmpty()) {
                log.info("ℹ️ Немає активних станцій для запуску трансляцій");
                return;
            }

            log.info("📋 Знайдено {} активних станцій", onlineStations.size());

            for (StationDto station : onlineStations) {
                String stationId = String.valueOf(station.getId());
                Optional<JanusLiveStream> existingStream = portService.getStreamByStation(stationId);

                if (existingStream.isEmpty()) {
                    // Станція онлайн, але трансляція не запущена → запускаємо
                    log.info("▶️ Станція '{}' не має активної трансляції. Запускаємо...",
                            station.getName());
                    try {
                        startBroadcast(stationId);
                        log.info("✅ Трансляцію для станції '{}' запущено", station.getName());
                    } catch (Exception e) {
                        log.error("❌ Помилка запуску трансляції для станції '{}': {}",
                                station.getName(), e.getMessage(), e);
                    }
                } else {
                    // Трансляція вже активна
                    log.info("✅ Станція '{}' вже має активну трансляцію на порту {}",
                            station.getName(), existingStream.get().getRtpPort());
                }
            }

            log.info("✅ Ініціалізацію трансляцій завершено");

        } catch (Exception e) {
            log.error("❌ Критична помилка ініціалізації трансляцій", e);
        }
    }

    // ================================================================
    // 🏠 СТВОРЕННЯ СЛУЖБОВИХ КІМНАТ СТАНЦІЇ
    // ================================================================

    /**
     * ✅ НОВИЙ МЕТОД: Гарантує, що ВСІ 4 кімнати станції існують у Janus.
     *
     * Викликається при:
     *   - Підготовці ефіру (натискання "Підготувати ефір" у панелі модератора)
     *   - Переході станції в ONLINE
     *   - Вході модератора в панель керування (опціонально)
     *
     * Метод є ідемпотентним: якщо кімната вже існує в Janus,
     * createAudioBridgeRoom() просто логує попередження і продовжує.
     *
     * @param stationUuid UUID станції, для якої створюються кімнати
     */
    @Transactional
    public void ensureAllStationRoomsExist(String stationUuid) {
        Station station = stationRepo.getStationByUuid(stationUuid);
        if (station == null) {
            log.warn("⚠️ Станцію {} не знайдено. Неможливо створити службові кімнати.", stationUuid);
            return;
        }

        log.info("🏠 Перевірка/створення службових кімнат для станції '{}' ({})",
                station.getName(), stationUuid);

        // Масив UUID всіх кімнат станції з їхніми типами для логування
        String[][] rooms = {
                {station.getRoomuuid(), "MAIN"},
                {station.getGreenRoomUuid(), "GREEN"},
                {station.getBroadcastRoomUuid(), "BROADCAST"},
                {station.getStaffRoomUuid(), "STAFF"}
        };

        int createdCount = 0;
        int skippedCount = 0;

        for (String[] roomInfo : rooms) {
            String roomUuid = roomInfo[0];
            String roomType = roomInfo[1];

            if (roomUuid == null || roomUuid.isEmpty()) {
                log.debug("⏭️ {} кімната не налаштована для станції {}. Пропускаємо.",
                        roomType, stationUuid);
                skippedCount++;
                continue;
            }

            try {
                MessageRoom messageRoom = messageRoomRepository.getByUuid(roomUuid);
                if (messageRoom == null) {
                    log.warn("⚠️ MessageRoom з UUID {} ({}) не знайдено в БД. Пропускаємо.",
                            roomUuid, roomType);
                    skippedCount++;
                    continue;
                }

                // createAudioBridgeRoom() внутрішньо обробляє "already exists"
                // тому цей виклик безпечний для повторного використання
                roomService.createAudioBridgeRoom(messageRoom.getId(), messageRoom.getRoomname());
                createdCount++;

            } catch (Exception e) {
                log.error("❌ Помилка створення {} кімнати ({}) для станції {}: {}",
                        roomType, roomUuid, stationUuid, e.getMessage());
            }
        }

        log.info("✅ Службові кімнати станції '{}': {} створено/перевірено, {} пропущено",
                station.getName(), createdCount, skippedCount);
    }

    // ================================================================
    // ▶️ ЗАПУСК ТРАНСЛЯЦІЇ
    // ================================================================

    /**
     * Запускає трансляцію радіостанції в Janus AudioBridge.
     *
     * Процес:
     *   1. Виділяє вільний UDP порт для RTP
     *   2. Створює AudioBridge room у Janus (якщо ще не існує)
     *   3. Створює персистентну сесію та приєднує Plain RTP учасника
     *   4. Надсилає команду START до FFmpeg агента через RabbitMQ
     *   5. Зберігає стан у БД (JanusLiveStream)
     *
     * @param stationId ID станції (числовий, як рядок)
     * @return JanusLiveStream з даними про активну трансляцію
     */
    @Transactional
    public JanusLiveStream startBroadcast(String stationId) {
        return portService.getStreamByStation(stationId).orElseGet(() -> {
            log.info("🎬 === startBroadcast() === stationId={}", stationId);

            String sourceStreamUrl = "http://icecast.rfa:8000/ukraine";

            // Крок 1: Виділяємо вільний UDP порт
            Integer freePort = portService.findNextFreePort();
            log.info("🔌 Виділено UDP порт: {}", freePort);

            // Крок 2: Визначаємо Janus Room ID
            Long janusRoomId = Long.parseLong(stationId.replaceAll("[^0-9]", ""));
            log.info("🏠 Janus Room ID: {} (з stationId='{}')", janusRoomId, stationId);

            // Крок 3: Створюємо аудіокімнату в Janus
            log.info("📤 [КРОК 1] Створення аудіокімнати в Janus...");
            try {
                roomService.createAudioBridgeRoom(janusRoomId, "Ефір: " + stationId);
                log.info("✅ [КРОК 1] Кімнату {} створено", janusRoomId);
            } catch (Exception e) {
                log.error("❌ [КРОК 1] Помилка створення кімнати {}: {}", janusRoomId, e.getMessage(), e);
                throw e;
            }

            // Крок 4: Створюємо персистентну сесію та приєднуємо Plain RTP
            log.info("📤 [КРОК 2] Створення сесії та приєднання Plain RTP...");
            Long sessionId = null;
            Long handleId = null;
            Integer janusRtpPort = null;

            try {
                sessionId = sessionManager.createSession();
                handleId = sessionManager.attachPlugin(sessionId, "janus.plugin.audiobridge");

                Map<String, Object> joinResponse = participantService.joinAsRtpParticipant(
                        sessionId, handleId, janusRoomId,
                        "Радіо Ефір", ffmpegAgentHost, freePort
                );

                // Парсимо відповідь для отримання порту Janus
                Map<String, Object> plugindata = (Map<String, Object>) joinResponse.get("plugindata");
                Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
                Map<String, Object> rtp = (Map<String, Object>) data.get("rtp");
                janusRtpPort = ((Number) rtp.get("port")).intValue();

                log.info("✅ [КРОК 2] RTP приєднано: session={}, handle={}, janusPort={}",
                        sessionId, handleId, janusRtpPort);

            } catch (Exception e) {
                log.error("❌ [КРОК 2] Помилка приєднання Plain RTP: {}", e.getMessage(), e);
                // Очищаємо сесію при помилці
                if (sessionId != null) {
                    try { sessionManager.destroySession(sessionId); } catch (Exception ex) { /* ignore */ }
                }
                throw new RuntimeException("Помилка приєднання Plain RTP: " + e.getMessage(), e);
            }

            // Крок 5: Надсилаємо команду START до FFmpeg агента
            String rtpUrl = String.format(
                    "rtp://janus.rfa:%d?localport=%d&pkt_size=1200",
                    janusRtpPort, freePort
            );
            log.info("🔗 RTP URL для FFmpeg: {}", rtpUrl);

            log.info("📤 [КРОК 3] Відправка команди START до FFmpeg агента...");
            try {
                ffmpegService.sendStartCommand(stationId, sourceStreamUrl, rtpUrl);
                log.info("✅ [КРОК 3] Команду FFmpeg відправлено в RabbitMQ");
            } catch (Exception e) {
                log.error("❌ [КРОК 3] Помилка відправки команди FFmpeg: {}", e.getMessage(), e);
                if (sessionId != null) {
                    try { sessionManager.destroySession(sessionId); } catch (Exception ex) { /* ignore */ }
                }
                throw e;
            }

            // Крок 6: Зберігаємо стан у БД
            log.info("💾 [КРОК 4] Збереження стану в БД...");
            try {
                JanusLiveStream savedStream = portService.saveActiveStream(
                        stationId, janusRoomId, freePort, 0, null
                );
                savedStream.setJanusSessionId(sessionId);
                savedStream.setJanusHandleId(handleId);
                portService.saveStream(savedStream);

                log.info("🎉 === Трансляцію успішно запущено === " +
                                "stationId={}, rtpPort={}, janusRtpPort={}",
                        stationId, freePort, janusRtpPort);

                return savedStream;

            } catch (Exception e) {
                log.error("❌ [КРОК 4] Помилка збереження в БД: {}", e.getMessage(), e);
                if (sessionId != null) {
                    try { sessionManager.destroySession(sessionId); } catch (Exception ex) { /* ignore */ }
                }
                throw e;
            }
        });
    }

    // ================================================================
    // ⏹️ ЗУПИНКА ТРАНСЛЯЦІЇ
    // ================================================================

    /**
     * Зупиняє трансляцію радіостанції.
     *
     * Процес:
     *   1. Надсилає команду STOP до FFmpeg агента
     *   2. Знищує персистентну сесію Janus (якщо є)
     *   3. Видаляє запис про стрім з БД (звільняє UDP порт)
     *
     * @param stationId ID станції (числовий, як рядок)
     */
    @Transactional
    public void stopBroadcast(String stationId) {
        portService.getStreamByStation(stationId).ifPresent(stream -> {
            log.info("⏹️ Зупинка трансляції: {}", stationId);

            // Зупиняємо FFmpeg
            ffmpegService.sendStopCommand(stationId);

            // Знищуємо сесію Janus (Plain RTP учасник)
            if (stream.getJanusSessionId() != null) {
                try {
                    sessionManager.destroySession(stream.getJanusSessionId());
                    log.info("✅ Сесію Janus {} знищено для Plain RTP", stream.getJanusSessionId());
                } catch (Exception e) {
                    log.warn("⚠️ Помилка знищення сесії Janus: {}", e.getMessage());
                }
            }

            // Видаляємо запис з БД (звільняємо UDP порт)
            portService.deleteStream(stationId);
            log.info("✅ Трансляцію {} зупинено, порт звільнено", stationId);
        });
    }
}