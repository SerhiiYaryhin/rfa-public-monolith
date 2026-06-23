package media.toloka.rfa.media.messanger.webrtc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import media.toloka.rfa.media.messanger.model.MessageRoom;
import media.toloka.rfa.media.messanger.model.enumerate.EChatRoomType;
import media.toloka.rfa.media.messanger.service.MessangerService;
import media.toloka.rfa.media.messanger.webrtc.model.dto.StationDto;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.media.messanger.webrtc.model.JanusLiveStream;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.station.service.StationService;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Високорівневий сервіс для керування прямими ефірами через WebRTC.
 * 
 * Цей сервіс координує роботу декількох систем:
 * 1. Janus (через JanusRabbitClient) — створення медіа-кімнати.
 * 2. Порти (через PortAllocationService) — виділення вільного UDP порту.
 * 3. FFmpeg Агент (через RabbitMQ) — запуск процесу перекодування аудіопотоку.
 */
@Service
public class RadioLiveService {

    private final JanusRabbitClient janusClient;
    private final PortAllocationService portService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    private ClientService clientService;
    @Autowired
    private MessangerService messangerService;
    @Autowired
    private StationService stationService;

    private static final Logger log = LoggerFactory.getLogger(RadioLiveService.class);


    public RadioLiveService(JanusRabbitClient janusClient,
                            PortAllocationService portService,
                            RabbitTemplate rabbitTemplate,
                            ObjectMapper objectMapper) {
        this.janusClient = janusClient;
        this.portService = portService;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Ініціалізація сервера.
     * При старті сервера чистимо всі аудіо кімнати в Janus
     * та створюємо кімнати для радіостанцій з онлайном.
     *
     */
    @Transactional
    public void clearAndCreate() {

    }

    /**
     * Запуск трансляції радіостанції в WebRTC ефір.
     * @param stationId ID станції (наприклад, 'rfa-rock')
     * @param sourceStreamUrl URL джерела (наприклад, Icecast потік)
     */
    @Transactional
    public JanusLiveStream startBroadcast(String stationId, String sourceStreamUrl) {
        // Якщо ефір вже запущено — повертаємо існуючий стрім
        return portService.getStreamByStation(stationId).orElseGet(() -> {
            log.info("Потрапили в незапущену станцію: {}",stationId);
            // Шукаємо вільний порт
            Integer freePort = portService.findNextFreePort();
            log.info("Порт для трансляції на Janus: {}",freePort);

            // 1. Створюємо кімнату в Janus з підтримкою RTP на виділеному порту
            Long janusRoomId = janusClient.createDynamicRoomWithRadio("Ефір: " + stationId, freePort);
            log.info("ID кімнати в Янус: {}", janusRoomId);

            // 2. Надсилаємо команду FFmpeg Агенту для запуску трансляції.
            // FFmpeg почне читати sourceStreamUrl і пересилати його в Opus/RTP на janus.rfa:freePort
            sendFfmpegCommand("start", stationId, sourceStreamUrl, "rtp://janus.rfa:" + freePort);

            // 3. Зберігаємо стан у базу даних
            return portService.saveActiveStream(stationId, janusRoomId, freePort, 0);
        });
    }

    /**
     * Зупинка трансляції.
     * Припиняє роботу FFmpeg та видаляє запис про стрім, звільняючи UDP порт.
     */
    @Transactional
    public void stopBroadcast(String stationId) {
        portService.getStreamByStation(stationId).ifPresent(stream -> {
            // 1. Команда Агенту на зупинку процесу
            sendFfmpegCommand("stop", stationId, null, null);

            // 2. Видаляємо запис з БД
            portService.deleteStream(stationId);
        });
    }

    /**
     * Отримує унікальний токен користувача та реєструє його в Janus.
     * Це дозволяє фронтенду підключатися без знання статичного api_secret.
     */
    public String generateAndRegisterToken() {
//        String token = "rfa-token-" + java.util.UUID.randomUUID().toString().substring(0, 12);

        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null)  return null;
        String token = cd.getUuid();

        janusClient.registerUserToken(token);
        return token;
    }

    /**
     * Перевірка зв'язку з Janus Admin API.
     */
    public boolean pingJanus() {
        return janusClient.pingJanus();
    }

    /**
     * Отримання списку всіх активних кімнат AudioBridge та їхніх учасників.
     * Збагачує дані Janus інформацією про трансляцію радіостанції з локальної БД.
     */
    public List<Map<String, Object>> listRoomsAndParticipants() {
        List<Map<String, Object>> janusRooms = janusClient.getJanusRoomsAndParticipants();
        
        // Збагачуємо інформацію про кімнати даними з нашої БД
        for (Map<String, Object> room : janusRooms) {
            Long janusRoomId = ((Number) room.get("room")).longValue();
            Optional<JanusLiveStream> liveStreamOpt = portService.getStreamByJanusRoomId(janusRoomId);
            
            if (liveStreamOpt.isPresent()) {
                JanusLiveStream liveStream = liveStreamOpt.get();
                room.put("is_streaming", true);
                room.put("station_id", liveStream.getStationId());
                room.put("rtp_port", liveStream.getRtpPort());
            } else {
                room.put("is_streaming", false);
            }
        }
        return janusRooms;
    }

    /**
     * Формує та надсилає JSON-команду для FFmpeg Агента через RabbitMQ.
     */
    private void sendFfmpegCommand(String command, String stationId, String streamUrl, String rtpUrl) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("command", command);
            message.put("stationId", stationId);
            if ("start".equals(command)) {
                message.put("streamUrl", streamUrl);
                message.put("rtpUrl", rtpUrl);
            }

            // Серіалізуємо в JSON та відправляємо в чергу
            String jsonPayload = objectMapper.writeValueAsString(message);
            log.info("Відправляємо в Rabbit: {}",jsonPayload);
            rabbitTemplate.convertAndSend("ffmpeg-agent-commands", jsonPayload);

        } catch (Exception e) {
            throw new RuntimeException("Помилка відправки команди FFmpeg: " + e.getMessage());
        }
    }

    /**
     * Отримання повної інформації про активні кімнати Janus, їхніх учасників
     * та статус трансляції (чи підключено Icecast-потік до цієї кімнати).
     *
     */

    @Transactional
    public List<StationDto> getActiveRoomsInfo() {
        // 1. Джерело правди — База Даних: Отримуємо станції з онлайн-кімнатами
        List<StationDto> activeStations = stationService.getActiveStations();

        // Створюємо Set для швидкого пошуку валідних номерів кімнат (id кімнати в базі є ID для Janus)
        Set<Long> validRoomIds = activeStations.stream()
                .map(StationDto::getId) // Якщо ID станції слугує номером кімнати в Janus
                .collect(Collectors.toSet());

        // 2. Отримуємо те, що зараз реально крутиться в пам'яті Janus
        List<Map<String, Object>> janusRooms;
        try {
            janusRooms = janusClient.getJanusRoomsAndParticipants();
        } catch (Exception e) {
            log.error("Помилка отримання даних з Janus, повертаємо чисті дані з БД", e);
            return activeStations; // fallback: повертаємо хоча б структуру з бази
        }

        // Перетворюємо дані Janus у зручну мапу: Key = roomId, Value = дані кімнати (учасники тощо)
        Map<Long, Map<String, Object>> janusRoomsMap = new HashMap<>();
        for (Map<String, Object> jRoom : janusRooms) {
            Number roomIdNum = (Number) jRoom.get("room");
            if (roomIdNum != null) {
                janusRoomsMap.put(roomIdNum.longValue(), jRoom);
            }
        }

        // 3. СИНХРОНІЗАЦІЯ ТА ОЧИЩЕННЯ ЗАЙВИХ КІМНАТ (Актуалізація)
        for (Long janusRoomId : janusRoomsMap.keySet()) {
            if (!validRoomIds.contains(janusRoomId)) {
                // Кімната є в Janus, але в нашій Базі Даних вона вже НЕ в онлайні!
                log.warn("Виявлено зайву кімнату в Janus з ID: {}. Починаємо очищення...", janusRoomId);

                Map<String, Object> orphanRoom = janusRoomsMap.get(janusRoomId);
                List<?> participants = (List<?>) orphanRoom.get("participants");

                // Евакуація користувачів у головну кімнату чату
                if (participants != null && !participants.isEmpty()) {
                    log.info("Евакуація {} користувачів із кімнати {} до головного чату", participants.size(), janusRoomId);
                    try {
                        // Ваш месенджер-сервіс переводить людей в дефолтну кімнату
                        messangerService.moveUsersToMainChatRoom(janusRoomId);
                    } catch (Exception e) {
                        log.error("Не вдалося перемістити користувачів у головний чат", e);
                    }
                }

                // Видаляємо аудіокімнату з самого Janus, щоб не жерти ресурси
                try {
                    janusClient.destroyAudioBridgeRoom(janusRoomId);
                    log.info("Кімнату {} успішно видалено з Janus", janusRoomId);
                } catch (Exception e) {
                    log.error("Помилка видалення кімнати з Janus", e);
                }
            }
        }

        // 4. ЗБАГАЧЕННЯ ДАНИХ ДЛЯ ФРОНТЕНДУ
        // Проходимо по наших активних станціях і додаємо до них динамічні дані (список учасників) з Janus
        for (StationDto station : activeStations) {
            Long roomId = station.getId(); // Або station.getRoomuuid(), залежно від того, що є ідентифікатором кімнати в Janus

            Map<String, Object> jRoomData = janusRoomsMap.get(roomId);
            if (jRoomData != null) {
                // Якщо Janus знає про цю кімнату, додаємо прапорець стрімінгу та список людей
                station.setStationstate(true); // або використовуйте поле існуючого DTO
                // Можна розширити StationDto полем `private Object participants;`
                // station.setParticipants(jRoomData.get("participants"));
            } else {
                // Якщо в базі стоїть статус "онлайн", а Janus про неї ще не знає — треба створити її в Janus на льоту!
                try {
                    log.info("Кімната {} активна в БД, але відсутня в Janus. Створюємо...", roomId);
                    janusClient.createAudioBridgeRoom(roomId, station.getName());
                } catch (Exception e) {
                    log.error("Не вдалося створити кімнату на льоту в Janus", e);
                }
            }
        }

        return activeStations;
    }

//    @Transactional(readOnly = true)
//    public List<Map<String, Object>> getActiveRoomsInfo() {
//        List<Map<String, Object>> rooms;
//        try {
//            // отримали перелік активних кімнат з Janus
//            rooms = janusClient.getJanusRoomsAndParticipants();
//        } catch (Exception e) {
//            rooms = Collections.emptyList();
//        }
//
//        // Отримуємо перелік кімнат радіостанцій з прямим ефіром з бази
//        List<MessageRoom> liveRoom = messangerService.GetLiveChatRoomList(EChatRoomType.CHATROOM_TYPE_STATION);
//        // шукаємо наші кімнати з сайту в Janus. Якщо нема - створюємо. Якщо є зайві - знищуємо.
//
//
//        Map<Long, JanusLiveStream> activeStreamsMap = new HashMap<>();
//        try {
//            for (JanusLiveStream stream : portService.getAllActiveStreams()) {
//                activeStreamsMap.put(stream.getJanusRoomId(), stream);
//            }
//        } catch (Exception e) {
//            // DB error fallback
//        }
//
//        for (Map<String, Object> room : rooms) {
//            Number roomIdNum = (Number) room.get("room");
//            if (roomIdNum != null) {
//                Long roomId = roomIdNum.longValue();
//                JanusLiveStream activeStream = activeStreamsMap.get(roomId);
//                if (activeStream != null) {
//                    room.put("is_streaming", true);
//                    room.put("station_id", activeStream.getStationId());
//                } else {
//                    room.put("is_streaming", false);
//                }
//            }
//        }
//
//        return rooms;
//    }

    /**
     * Отримання глобального api_secret медіасервера.
     */
    public String getApiSecret() {
        return janusClient.getApiSecret();
    }
}
