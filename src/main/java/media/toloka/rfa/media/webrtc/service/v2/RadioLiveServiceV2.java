package media.toloka.rfa.media.webrtc.service.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import media.toloka.rfa.media.messanger.service.MessangerService;

import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.station.service.StationService;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import media.toloka.rfa.media.webrtc.model.JanusLiveStream;
import media.toloka.rfa.media.webrtc.model.dto.StationDto;
import media.toloka.rfa.media.webrtc.service.PortAllocationService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 📡 Високорівневий сервіс для керування голосовими кімнатами та трансляцією ефіру (версія v2).
 *
 * Основні функції:
 * - Запуск та зупинка трансляції інтернет-радіо в Janus через FFmpeg Plain RTP.
 * - Управління життєвим циклом кімнат та сесій Janus (Heartbeat/Keepalive).
 * - Самовідновлення трансляцій при перезапуску медіасервера Janus Gateway.
 * - Управління модерацією: вимкнення мікрофонів (Mute), вигнання (Kick), переміщення (Move), RTP Forward.
 */
@Service
public class RadioLiveServiceV2 {

    // Клієнт для зв'язку з Janus Gateway через RabbitMQ
    //private final media.toloka.rfa.media.messanger.webrtc.service.v2.JanusRabbitClientOptimized janusClient;
    private final media.toloka.rfa.media.messanger.webrtc.service.v2.JanusGatewayFacade janusClient;
    
    // Сервіс виділення та трекінгу UDP-портів для FFmpeg
    private final PortAllocationService portService;
    
    // Шаблон для відправки команд в RabbitMQ чергу FFmpeg агента
    private final RabbitTemplate rabbitTemplate;
    
    // Серіалізатор Jackson для конвертації команд у JSON
    private final ObjectMapper objectMapper;

    @Autowired
    private ClientService clientService;

    @Autowired
    private MessangerService messangerService;

    @Autowired
    private StationService stationService;

    private static final Logger log = LoggerFactory.getLogger(RadioLiveServiceV2.class);

    /**
     * Конструктор з впровадженням залежностей.
     */
    public RadioLiveServiceV2(
            media.toloka.rfa.media.messanger.webrtc.service.v2.JanusGatewayFacade janusClient,
            //media.toloka.rfa.media.messanger.webrtc.service.v2.JanusRabbitClientOptimized janusClient,
            PortAllocationService portService,
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper) {
        this.janusClient = janusClient;
        this.portService = portService;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    // ============================================================
    // ІНІЦІАЛІЗАЦІЯ ПРИ СТАРТІ СЕРВЕРА
    // ============================================================

    /**
     * 🚀 Подія ApplicationReadyEvent: виконується автоматично після повного старту Spring Boot.
     * Забезпечує автозапуск трансляцій для всіх станцій, які є активними ("online") в БД.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeBroadcasts() {
        log.info("🚀 Ініціалізація трансляцій при старті сервера...");

        try {
            // Отримуємо всі активні станції з бази даних
            List<StationDto> onlineStations = stationService.getActiveStations();

            if (onlineStations.isEmpty()) {
                log.info("Немає активних станцій для запуску трансляцій");
                return;
            }

            log.info("Знайдено {} активних станцій", onlineStations.size());

            // Для кожної активної станції перевіряємо, чи запущено трансляцію
            for (StationDto station : onlineStations) {
                String stationId = String.valueOf(station.getId());

                Optional<JanusLiveStream> existingStream = portService.getStreamByStation(stationId);

                if (existingStream.isEmpty()) {
                    log.info("Станція {} не має активної трансляції. Запускаємо...", station.getName());
                    try {
                        // Запускаємо трансляцію Plain RTP
                        startBroadcast(stationId);
                        log.info("✅ Трансляцію для станції {} запущено", station.getName());
                    } catch (Exception e) {
                        log.error("❌ Помилка запуску трансляції для станції {}: {}",
                                station.getName(), e.getMessage(), e);
                    }
                } else {
                    log.info("Станція {} вже має активну трансляцію на порту {}",
                            station.getName(), existingStream.get().getRtpPort());
                }
            }

            log.info("✅ Ініціалізацію трансляцій завершено");

        } catch (Exception e) {
            log.error("❌ Помилка ініціалізації трансляцій", e);
        }
    }

    // ============================================================
    // УПРАВЛІННЯ ТРАНСЛЯЦІЯМИ
    // ============================================================

//    @Transactional
//    public JanusLiveStream startBroadcast(String stationId) {
//        return portService.getStreamByStation(stationId).orElseGet(() -> {
//            log.info("🎬 === startBroadcast() === stationId={}", stationId);
//
//            String sourceStreamUrl = "http://icecast.rfa:8000/ukraine";
//            log.info("📻 URL потоку станції: {}", sourceStreamUrl);
//
//            Long janusRoomId = Long.parseLong(stationId.replaceAll("[^0-9]", ""));
//            log.info("🏠 Janus Room ID: {}", janusRoomId);
//
//            // Крок 1: Створюємо кімнату з allow_rtp_participants=true
//            log.info("📤 [КРОК 1] Створення аудіокімнати в Janus...");
//            try {
//                janusClient.createAudioBridgeRoom(janusRoomId, "Ефір: " + stationId);
//                log.info("✅ [КРОК 1] Кімнату {} створено", janusRoomId);
//            } catch (Exception e) {
//                log.error("❌ [КРОК 1] Помилка створення кімнати {}: {}", janusRoomId, e.getMessage(), e);
//                throw e;
//            }
//
//            // Крок 2: ✅ НОВЕ - Створюємо RTP учасника
//            log.info("📤 [КРОК 2] Створення RTP учасника для станції {}...", stationId);
//            Map<String, Object> rtpParticipant;
//            try {
//                rtpParticipant = janusClient.createRtpParticipant(
//                        janusRoomId,
//                        stationId,
//                        "Радіостанція " + stationId
//                );
//                log.info("✅ [КРОК 2] RTP учасника створено: ip={}, port={}",
//                        rtpParticipant.get("ip"), rtpParticipant.get("port"));
//            } catch (Exception e) {
//                log.error("❌ [КРОК 2] Помилка створення RTP учасника: {}", e.getMessage(), e);
//                throw e;
//            }
//
//            // Крок 3: ✅ Формуємо RTP URL для FFmpeg (отриманий від Janus)
//            String rtpIp = (String) rtpParticipant.get("ip");
//            Integer rtpPort = (Integer) rtpParticipant.get("port");
//            String rtpUrl = String.format("rtp://%s:%d?pkt_size=1200", rtpIp, rtpPort);
//            log.info("🔗 RTP URL для FFmpeg: {}", rtpUrl);
//
//            // Крок 4: Відправка команди FFmpeg
//            log.info("📤 [КРОК 3] Відправка команди START до FFmpeg агента...");
//            try {
//                sendFfmpegCommand("start", stationId, sourceStreamUrl, rtpUrl);
//                log.info("✅ [КРОК 3] Команду FFmpeg відправлено в RabbitMQ");
//            } catch (Exception e) {
//                log.error("❌ [КРОК 3] Помилка відправки команди FFmpeg: {}", e.getMessage(), e);
//                throw e;
//            }
//
//            // Крок 5: Збереження в БД
//            log.info("💾 [КРОК 4] Збереження стану в БД...");
//            try {
//                // ✅ Використовуємо порт, який повернув Janus
//                JanusLiveStream savedStream = portService.saveActiveStream(
//                        stationId, janusRoomId, rtpPort, 0, 0L);
//                log.info("✅ [КРОК 4] Збережено: id={}, stationId={}, janusRoomId={}, rtpPort={}",
//                        savedStream.getId(), savedStream.getStationId(),
//                        savedStream.getJanusRoomId(), savedStream.getRtpPort());
//
//                log.info("🎉 === Трансляцію успішно запущено === stationId={}, rtpPort={}",
//                        stationId, rtpPort);
//
//                return savedStream;
//            } catch (Exception e) {
//                log.error("❌ [КРОК 4] Помилка збереження в БД: {}", e.getMessage(), e);
//                throw e;
//            }
//        });
//    }

    /**
     * ✅ Запуск трансляції радіо як Plain RTP учасника кімнати AudioBridge.
     *
     * Порядок виконання:
     * 1. Виділяємо вільний локальний UDP порт через PortAllocationService (звідки FFmpeg надсилатиме RTP).
     * 2. Створюємо аудіокімнату в Janus (з дозволом на RTP учасників).
     * 3. Створюємо персистентну сесію та хендл плагіна на бекенді.
     * 4. Надсилаємо запит "join" з параметрами RTP. Janus повертає порт, на якому він чекає потік.
     * 5. Формуємо RTP URL з параметром localport для проходження Strict Source Port валідації в Janus.
     * 6. Надсилаємо команду старту FFmpeg в RabbitMQ чергу агента.
     * 7. Зберігаємо інформацію про сесію та порти в базу даних.
     */
    @Transactional
    public JanusLiveStream startBroadcast(String stationId) {
        return portService.getStreamByStation(stationId).orElseGet(() -> {
            log.info("🎬 === startBroadcast() === stationId={}", stationId); // todo 261015 Замінити на uuid

            String sourceStreamUrl = "http://icecast.rfa:8000/ukraine"; // todo 261015 Використати реальну адресу станції
            // Отримуємо вільний локальний UDP порт для біндингу FFmpeg
            Integer freePort = portService.findNextFreePort();
            log.info("🔌 Виділено UDP порт: {}", freePort);

            // Кімната в Janus відповідає ID станції
            Long janusRoomId = Long.parseLong(stationId.replaceAll("[^0-9]", "")); // адаптувати для uuid
            log.info("🏠 Janus Room ID: {} (з stationId='{}')", janusRoomId, stationId);

            // Крок 1: Створення кімнати в Janus Gateway
            log.info("📤 [КРОК 1] Створення аудіокімнати в Janus...");
            try {
                janusClient.createAudioBridgeRoom(janusRoomId, "Ефір: " + stationId);
                log.info("✅ [КРОК 1] Кімнату {} створено", janusRoomId);
            } catch (Exception e) {
                log.error("❌ [КРОК 1] Помилка створення кімнати {}: {}", janusRoomId, e.getMessage(), e);
                throw e;
            }

            // Крок 2: Створення сесії та хендлу, підключення Plain RTP учасника
            log.info("📤 [КРОК 2] Створення персистентної сесії та приєднання Plain RTP...");
            Long sessionId = null;
            Long handleId = null;
            Integer janusRtpPort = null;
            try {
                // Створюємо постійну сесію для підтримки зв'язку з RTP учасником
                sessionId = janusClient.createSession();
                handleId = janusClient.attachPlugin(sessionId, "janus.plugin.audiobridge");

                // Робимо join як RTP учасник. Передаємо freePort, з якого FFmpeg надсилатиме дані.
                Map<String, Object> joinResponse = janusClient.joinAsRtpParticipant(
                        sessionId, handleId, janusRoomId, "Радіо Ефір", "127.0.0.1", freePort
                );

                // Витягуємо порт на стороні Janus з відповіді API
                Map<String, Object> plugindata = (Map<String, Object>) joinResponse.get("plugindata");
                Map<String, Object> data = (Map<String, Object>) plugindata.get("data");
                Map<String, Object> rtp = (Map<String, Object>) data.get("rtp");
                janusRtpPort = ((Number) rtp.get("port")).intValue();

                log.info("✅ [КРОК 2] RTP приєднано: session={}, handle={}, port={}", sessionId, handleId, janusRtpPort);
            } catch (Exception e) {
                log.error("❌ [КРОК 2] Помилка приєднання Plain RTP: {}", e.getMessage(), e);
                if (sessionId != null) {
                    try { janusClient.destroySession(sessionId); } catch (Exception ex) {}
                }
                throw new RuntimeException("Помилка приєднання Plain RTP: " + e.getMessage(), e);
            }

            // Крок 3: Формуємо URL для FFmpeg. Параметр localport є критичним — він змушує FFmpeg відправляти потік з freePort.
            String rtpUrl = String.format("rtp://janus.rfa:%d?localport=%d&pkt_size=1200", janusRtpPort, freePort);
            log.info("🔗 RTP URL для FFmpeg: {}", rtpUrl);

            // Крок 4: Запуск FFmpeg через відправку повідомлення в чергу RabbitMQ
            log.info("📤 [КРОК 3] Відправка команди START до FFmpeg агента...");
            try {
                sendFfmpegCommand("start", stationId, sourceStreamUrl, rtpUrl);
                log.info("✅ [КРОК 3] Команду FFmpeg відправлено в RabbitMQ");
            } catch (Exception e) {
                log.error("❌ [КРОК 3] Помилка відправки команди FFmpeg: {}", e.getMessage(), e);
                if (sessionId != null) {
                    try { janusClient.destroySession(sessionId); } catch (Exception ex) {}
                }
                throw e;
            }

            // Крок 5: Збереження стану трансляції у базі даних для відновлення та зупинки
            log.info("💾 [КРОК 4] Збереження стану в БД...");
            try {
                JanusLiveStream savedStream = portService.saveActiveStream(
                        stationId, janusRoomId, freePort, 0, null);
                savedStream.setJanusSessionId(sessionId);
                savedStream.setJanusHandleId(handleId);
                portService.saveStream(savedStream);

                log.info("🎉 === Трансляцію успішно запущено === stationId={}, rtpPort={}, janusRtpPort={}",
                        stationId, freePort, janusRtpPort);

                return savedStream;
            } catch (Exception e) {
                log.error("❌ [КРОК 4] Помилка збереження в БД: {}", e.getMessage(), e);
                if (sessionId != null) {
                    try { janusClient.destroySession(sessionId); } catch (Exception ex) {}
                }
                throw e;
            }
        });
    }


//    @Transactional
//    public void stopBroadcast(String stationId) {
//        portService.getStreamByStation(stationId).ifPresent(stream -> {
//            log.info("⏹️ Зупинка трансляції: {}", stationId);
//
//            // 1. Зупиняємо FFmpeg
//            sendFfmpegCommand("stop", stationId, null, null);
//
//            // 2. ✅ НОВЕ - Видаляємо RTP учасника
//            try {
//                janusClient.removeRtpParticipant(stream.getJanusRoomId(), stationId);
//                log.info("✅ RTP учасника видалено з кімнати {}", stream.getJanusRoomId());
//            } catch (Exception e) {
//                log.warn("⚠️ Помилка видалення RTP учасника: {}", e.getMessage());
//            }
//
//            // 3. Видаляємо з БД
//            portService.deleteStream(stationId);
//        });
//    }

    /**
     * ✅ Зупинка трансляції радіостанції.
     * 1. Надсилає команду зупинки FFmpeg агенту.
     * 2. Руйнує сесію Janus, що автоматично відключає Plain RTP учасника з кімнати.
     * 3. Видаляє запис про трансляцію та звільняє UDP порт в БД.
     */
    @Transactional
    public void stopBroadcast(String stationId) {
        portService.getStreamByStation(stationId).ifPresent(stream -> {
            log.info("⏹️ Зупинка трансляції: {}", stationId);

            // 1. Зупиняємо FFmpeg процес через RabbitMQ
            sendFfmpegCommand("stop", stationId, null, null);

            // 2. Закриваємо сесію Janus для від'єднання RTP учасника
            if (stream.getJanusSessionId() != null) {
                try {
                    janusClient.destroySession(stream.getJanusSessionId());
                    log.info("✅ Сесію Janus {} знищено для Plain RTP", stream.getJanusSessionId());
                } catch (Exception e) {
                    log.warn("⚠️ Помилка знищення сесії Janus: {}", e.getMessage());
                }
            }

            // 3. Видаляємо стрім з бази даних
            portService.deleteStream(stationId);
        });
    }

    // ============================================================
    // УПРАВЛІННЯ ТОКЕНАМИ
    // ============================================================

    /**
     * Генерація та реєстрація токена авторизації для WebRTC.
     * Використовує UUID поточного користувача як токен доступу в Janus.
     */
    public String generateAndRegisterToken() {
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) {
            log.warn("Користувач не авторизований");
            return null;
        }

        String token = cd.getUuid();
        log.info("Реєстрація токена користувача: {}", token);
        janusClient.registerUserToken(token);

        return token;
    }

    /**
     * Перевірка, чи зареєстрований токен поточного користувача в Janus.
     */
    @Transactional(readOnly = true)
    public Boolean checkUserTokenInJanus() {
        Users user = clientService.GetCurrentUser();
        if (user == null) return false;

        Clientdetail cd = clientService.GetClientDetailByUser(user);
        if (cd == null) return false;

        String userToken = cd.getUuid();
        boolean isRegistered = janusClient.checkUserTokenInJanus(userToken);

        log.info("Токен користувача {} {} в Janus",
                userToken, isRegistered ? "знайдено" : "НЕ знайдено");

        return isRegistered;
    }

    // ============================================================
    // ОТРИМАННЯ СПИСКУ КІМНАТ
    // ============================================================

    /**
     * Отримання списку активних кімнат разом із учасниками (синхронізація з Janus).
     *
     * Функціонал:
     * - Отримує список кімнат з Janus. Якщо Janus лежить, повертає дефолтні дані.
     * - Видаляє з Janus "осиротілі" кімнати, яких немає в БД (попередньо евакуювавши людей в дефолтний чат).
     * - Перевіряє наявність кімнат для кожної станції.
     * - **Самовідновлення:** Якщо кімната пропала в Janus (наприклад, після перезапуску Janus), 
     *   але трансляція радіо активна в БД — створює кімнату заново, перепідключає Plain RTP учасника,
     *   отримує новий RTP порт від Janus, оновлює БД та перезапускає FFmpeg.
     */
    @Transactional
    public List<StationDto> getActiveRoomsInfo() {
        log.debug("Отримання списку активних кімнат");

        List<StationDto> activeStations = stationService.getActiveStations();
        Set<Long> validRoomIds = activeStations.stream()
                .map(StationDto::getId)
                .collect(Collectors.toSet());

        List<Map<String, Object>> janusRooms;
        try {
            // Отримуємо реальні кімнати та учасників з медіасервера
            janusRooms = janusClient.getJanusRoomsAndParticipants();
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

        // Синхронізація: видаляємо зайві кімнати, які випадково залишилися в Janus
        for (Long janusRoomId : janusRoomsMap.keySet()) {
            if (!validRoomIds.contains(janusRoomId)) {
                log.warn("Зайва кімната в Janus: {}. Видаляємо...", janusRoomId);

                Map<String, Object> orphanRoom = janusRoomsMap.get(janusRoomId);
                List<?> participants = (List<?>) orphanRoom.get("participants");

                if (participants != null && !participants.isEmpty()) {
                    log.info("Евакуація {} користувачів з кімнати {}",
                            participants.size(), janusRoomId);
                    try {
                        messangerService.moveUsersToMainChatRoom(janusRoomId);
                    } catch (Exception e) {
                        log.error("Помилка евакуації користувачів", e);
                    }
                }

                try {
                    janusClient.destroyAudioBridgeRoom(janusRoomId);
                    log.info("Кімнату {} видалено з Janus", janusRoomId);
                } catch (Exception e) {
                    log.error("Помилка видалення кімнати {}", janusRoomId, e);
                }
            }
        }

        // Перевіряємо та збагачуємо дані про станції
        for (StationDto station : activeStations) {
            Long roomId = station.getId();
            String stationIdStr = String.valueOf(roomId);

            Map<String, Object> jRoomData = janusRoomsMap.get(roomId);

            if (jRoomData != null) {
                station.setStationstate(true);
                List<Object> participants = (List<Object>) jRoomData.get("participants");
                station.setParticipants(participants != null ? participants : new ArrayList<>());

            } else {
                // 🌀 САМОВІДНОВЛЕННЯ: Кімната пропала в Janus Gateway
                log.info("Кімната {} відсутня в Janus. Створюємо...", roomId);

                try {
                    // Створюємо кімнату заново
                    janusClient.createAudioBridgeRoom(roomId, station.getName());

                    // Перевіряємо, чи має ця станція бути в ефірі згідно з БД
                    Optional<JanusLiveStream> existingStream = portService.getStreamByStation(stationIdStr);
                    if (existingStream.isPresent()) {
                        JanusLiveStream stream = existingStream.get();
                        log.info("Відновлюємо Plain RTP для станції {}", stationIdStr);

                        try {
                            // Реєструємо нову сесію
                            Long sessionId = janusClient.createSession();
                            Long handleId = janusClient.attachPlugin(sessionId, "janus.plugin.audiobridge");

                            // Підключаємося як RTP учасник заново на раніше виділений локальний порт
                            Map<String, Object> joinResponse = janusClient.joinAsRtpParticipant(
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
                            sendFfmpegCommand("stop", stationIdStr, null, null);

                            String rtpUrl = String.format("rtp://janus.rfa:%d?localport=%d&pkt_size=1200", janusRtpPort, stream.getRtpPort());
                            String sourceStreamUrl = "http://icecast.rfa:8000/ukraine";
                            sendFfmpegCommand("start", stationIdStr, sourceStreamUrl, rtpUrl);

                            log.info("Plain RTP успішно відновлено для кімнати {}: session={}, handle={}, port={}",
                                    roomId, sessionId, handleId, janusRtpPort);
                        } catch (Exception ex) {
                            log.error("Помилка відновлення Plain RTP для кімнати {}: {}", roomId, ex.getMessage(), ex);
                        }
                    }

                    station.setStationstate(true);
                    station.setParticipants(new ArrayList<>());

                } catch (Exception e) {
                    log.error("Помилка створення кімнати {}", roomId, e);
                    station.setStationstate(false);
                }
            }
        }

        return activeStations;
    }

    // ============================================================
    // ПІДКЛЮЧЕННЯ КОРИСТУВАЧА ДО КІМНАТИ
    // ============================================================

    /**
     * Підготовка підключення користувача до аудіокімнати.
     * Автоматично знаходить і "виганяє" користувача з інших голосових кімнат Janus,
     * щоб запобігти подвійному підключенню під одним токеном.
     */
    public Map<String, Object> connectClientToAudioRoom(Long roomID) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            log.warn("Користувач не залогінений");
            return Map.of("success", false, "error", "Користувач не залогінений");
        }

        Clientdetail cd = clientService.GetClientDetailByUser(user);
        String userName = cd.getCustname() + " " + cd.getCustsurname();
        String userUUID = cd.getUuid();

        log.info("Підключення користувача {} (UUID: {}) до кімнати {}",
                userName, userUUID, roomID);

        try {
            // Отримуємо всі кімнати в Janus
            List<Long> activeJanusRooms = janusClient.getActiveRoomsIds();

            if (activeJanusRooms != null) {
                for (Long activeRoomId : activeJanusRooms) {
                    // Перевіряємо інші кімнати на присутність нашого токена
                    if (!activeRoomId.equals(roomID)) {
                        boolean isUserInRoom = janusClient.isUserInAudioRoom(activeRoomId, userUUID);

                        if (isUserInRoom) {
                            log.info("Користувач знайдений в кімнаті {}. Видаляємо...", activeRoomId);
                            // Виганяємо з попередньої кімнати
                            janusClient.kickUserFromAudioBridgeRoom(activeRoomId, userUUID);
                        }
                    }
                }
            }

            // Формуємо відповідь для фронтенду з авторизаційними параметрами
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("roomid", roomID);
            result.put("token", userUUID);
            result.put("display", userName);
            result.put("userId", cd.getId());

            return result;

        } catch (Exception e) {
            log.error("Помилка підключення до кімнати {}", roomID, e);
            return Map.of(
                    "success", false,
                    "error", "Помилка підключення: " + e.getMessage()
            );
        }
    }

    // ============================================================
    // МОДЕРАЦІЯ ТА RTP FORWARD
    // ============================================================

    /**
     * Зміна стану мікрофона учасника (Mute / Unmute).
     */
    public void muteParticipant(Long roomId, Long participantId, boolean mute) {
        janusClient.muteParticipant(roomId, participantId, mute);
    }

    /**
     * Вигнання учасника з кімнати за його ID в Janus.
     */
    public void kickParticipant(Long roomId, Long participantId) {
        janusClient.kickParticipant(roomId, participantId);
    }

    /**
     * Старт ретрансляції аудіопотоку кімнати назовні (RTP Forward).
     */
    public Long startRtpForward(Long roomId, String host, Integer port) {
        return janusClient.createRtpForward(roomId, host, port);
    }

    /**
     * Зупинка ретрансляції аудіопотоку (RTP Forward).
     */
    public void stopRtpForward(Long roomId, Long streamId) {
        janusClient.stopRtpForward(roomId, streamId);
    }

    // ============================================================
    // ДОПОМІЖНІ МЕТОДИ
    // ============================================================

    /**
     * Діагностичний пінг Janus Admin API.
     */
    public boolean pingJanus() {
        return janusClient.pingJanus();
    }

    /**
     * Отримання секретного ключа для API.
     */
    public String getApiSecret() {
        return janusClient.getApiSecret();
    }

    /**
     * Відправка команди старту або зупинки FFmpeg агенту через чергу RabbitMQ.
     */
    private void sendFfmpegCommand(String command, String stationId, String streamUrl, String rtpUrl) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("command", command);
            message.put("stationId", stationId);

            if ("start".equals(command)) {
                message.put("streamUrl", streamUrl);
                message.put("rtpUrl", rtpUrl);

                // Опції кодування аудіо (Opus, 48kHz, Stereo, VBR)
                message.put("ffmpegOptions", Map.of(
                        "acodec", "libopus",
                        "b:a", "128k",
                        "ar", "48000",
                        "ac", "2",
                        "application", "audio",
                        "vbr", "on",
                        "compression_level", "10"
                ));
            }

            String jsonPayload = objectMapper.writeValueAsString(message);
            log.info("📤 Відправка команди FFmpeg: {}", jsonPayload);

            // Публікація в RabbitMQ чергу "ffmpeg-agent-commands"
            rabbitTemplate.convertAndSend("ffmpeg-agent-commands", jsonPayload);
            log.info("✅ Команду FFmpeg відправлено в RabbitMQ");

        } catch (Exception e) {
            log.error("❌ Помилка відправки команди FFmpeg: {}", e.getMessage(), e);
            throw new RuntimeException("Помилка відправки команди FFmpeg: " + e.getMessage());
        }
    }

    // ============================================================
    // КЕЕPALIVE ДЛЯ ПЕРСИСТЕНТНИХ СЕСІЙ
    // ============================================================
    
    /**
     * 💓 Фоновий планувальник (Scheduler): Запускається кожні 20 секунд.
     * Надсилає запити "keepalive" для всіх активних сесій Plain RTP учасників.
     * Запобігає автоматичному таймауту сесії (60 сек) та відключенню радіо з кімнати.
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 20000)
    public void keepPersistentSessionsAlive() {
        try {
            List<JanusLiveStream> activeStreams = portService.getAllActiveStreams();
            for (JanusLiveStream stream : activeStreams) {
                if (stream.getJanusSessionId() != null) {
                    log.debug("Heartbeat: Надсилаємо keepalive для сесії {}", stream.getJanusSessionId());
                    janusClient.keepAlive(stream.getJanusSessionId());
                }
            }
        } catch (Exception e) {
            log.error("Помилка під час надсилання keepalive для сесій Janus: {}", e.getMessage());
        }
    }
}