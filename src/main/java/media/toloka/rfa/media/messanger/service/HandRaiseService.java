package media.toloka.rfa.media.messanger.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    /**
     * ✅ ВИПРАВЛЕНО: Додано BY_USER для коректної обробки самостійного опускання руки
     */
    public enum HandStatus {
        RAISED,     // Гість підняв руку
        ACCEPTED,   // Модератор прийняв (перемістив в Green Room)
        DECLINED,   // Модератор відхилив
        BY_USER     // Гість опустив руку самостійно
    }

    public static class HandRaiseInfo {
        public String userUuid;
        public String userName;
        public String roomUuid;
        public long timestamp;

        public HandRaiseInfo(String userUuid, String userName, String roomUuid) {
            this.userUuid = userUuid;
            this.userName = userName;
            this.roomUuid = roomUuid;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public void raiseHand(String stationUuid, String userUuid, String roomUuid) {
        Clientdetail cd = clientService.GetClientDetailByUUID(userUuid);
        String userName = (cd != null) ? (cd.getCustname() + " " + cd.getCustsurname()) : "Гість";

        Map<String, HandRaiseInfo> hands = stationHands.computeIfAbsent(stationUuid, k -> new ConcurrentHashMap<>());
        hands.put(userUuid, new HandRaiseInfo(userUuid, userName, roomUuid));

        log.info("🙋 Гість {} підняв руку в кімнаті {} (станція {})", userName, roomUuid, stationUuid);
        notifyModerators(stationUuid);
    }

    public void lowerHand(String stationUuid, String userUuid, HandStatus reason) {
        Map<String, HandRaiseInfo> hands = stationHands.get(stationUuid);
        HandRaiseInfo info = (hands != null) ? hands.remove(userUuid) : null;

        if (info != null) {
            log.info("✋ Гість {} опустив руку (станція {}, причина: {})", userUuid, stationUuid, reason);
            notifyModerators(stationUuid);
        }

        // Сповіщаємо самого гостя про зміну статусу (тільки якщо це не самостійне опускання)
        if (messagingTemplate != null && reason != null && reason != HandStatus.BY_USER) {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("targetUserUuid", userUuid);
                payload.put("status", reason.name());

                String message;
                switch (reason) {
                    case ACCEPTED:
                        message = "🎙️ Модератор запросив вас в Green Room";
                        break;
                    case DECLINED:
                        message = "❌ Модератор відхилив вашу руку";
                        break;
                    default:
                        message = "Рука опущена";
                }
                payload.put("message", message);

                String topic = "/topic/efir." + stationUuid + ".hand.status";
                messagingTemplate.convertAndSend(topic, payload);
                log.debug("📣 Сповіщено гостя {} про статус {}", userUuid, reason);
            } catch (Exception e) {
                log.warn("⚠️ Не вдалося сповістити гостя: {}", e.getMessage());
            }
        }
    }

    /**
     * Зручний метод без причини (для сумісності).
     */
    public void lowerHand(String stationUuid, String userUuid) {
        lowerHand(stationUuid, userUuid, HandStatus.BY_USER);
    }

    public List<HandRaiseInfo> getWaitingGuests(String stationUuid) {
        Map<String, HandRaiseInfo> hands = stationHands.get(stationUuid);
        if (hands == null) return Collections.emptyList();

        return hands.values().stream()
                .sorted(Comparator.comparingLong(h -> h.timestamp))
                .toList();
    }

    private void notifyModerators(String stationUuid) {
        if (messagingTemplate == null) return;

        List<HandRaiseInfo> guests = getWaitingGuests(stationUuid);

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