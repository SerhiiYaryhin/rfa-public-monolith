package media.toloka.rfa.media.messanger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🙋 Сервіс для керування станом "піднятої руки" в чаті.
 * Зберігає інформацію про те, хто і в якій кімнаті підняв руку,
 * та сповіщає панель модератора в реальному часі.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HandRaiseService {

    private final ClientService clientService;

    @Lazy
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // stationUuid -> Map<userUuid, HandRaiseInfo>
    private final Map<String, Map<String, HandRaiseInfo>> stationHands = new ConcurrentHashMap<>();

    public static class HandRaiseInfo {
        public String userUuid;
        public String userName;
        public String roomUuid; // Кімната, де піднято руку (Main або Green)
        public long timestamp;

        public HandRaiseInfo(String userUuid, String userName, String roomUuid) {
            this.userUuid = userUuid;
            this.userName = userName;
            this.roomUuid = roomUuid;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Гість піднімає руку.
     */
    public void raiseHand(String stationUuid, String userUuid, String roomUuid) {
        Clientdetail cd = clientService.GetClientDetailByUUID(userUuid);
        String userName = (cd != null) ? (cd.getCustname() + " " + cd.getCustsurname()) : "Гість";

        Map<String, HandRaiseInfo> hands = stationHands.computeIfAbsent(stationUuid, k -> new ConcurrentHashMap<>());
        hands.put(userUuid, new HandRaiseInfo(userUuid, userName, roomUuid));

        log.info("🙋 Гість {} підняв руку в кімнаті {} (станція {})", userName, roomUuid, stationUuid);

        notifyModerators(stationUuid);
    }

    /**
     * Гість (або модератор) опускає руку.
     */
    public void lowerHand(String stationUuid, String userUuid) {
        Map<String, HandRaiseInfo> hands = stationHands.get(stationUuid);
        if (hands != null && hands.remove(userUuid) != null) {
            log.info("✋ Гість {} опустив руку (станція {})", userUuid, stationUuid);
            notifyModerators(stationUuid);
        }
    }

    /**
     * Отримати список гостей, які підняли руку для конкретної станції.
     */
    public List<HandRaiseInfo> getWaitingGuests(String stationUuid) {
        Map<String, HandRaiseInfo> hands = stationHands.get(stationUuid);
        if (hands == null) return Collections.emptyList();

        // Сортуємо за часом підняття руки (найстаріші першими)
        return hands.values().stream()
                .sorted(Comparator.comparingLong(h -> h.timestamp))
                .toList();
    }

    /**
     * Сповіщаємо всіх модераторів станції про зміну списку очікуючих.
     */
    private void notifyModerators(String stationUuid) {
        if (messagingTemplate == null) return;

        List<HandRaiseInfo> guests = getWaitingGuests(stationUuid);

        // Формуємо простий JSON для фронтенду
        List<Map<String, String>> payload = new ArrayList<>();
        for (HandRaiseInfo info : guests) {
            Map<String, String> map = new HashMap<>();
            map.put("uuid", info.userUuid);
            map.put("name", info.userName);
            map.put("roomUuid", info.roomUuid);
            payload.add(map);
        }

        String topic = "/topic/efir." + stationUuid + ".waiting";
        messagingTemplate.convertAndSend(topic, payload);
        log.debug("📣 Оновлено список очікуючих для станції {}: {} гостей", stationUuid, guests.size());
    }
}