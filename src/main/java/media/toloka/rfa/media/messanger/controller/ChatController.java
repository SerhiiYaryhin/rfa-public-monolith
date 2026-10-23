package media.toloka.rfa.media.messanger.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import media.toloka.rfa.media.messanger.config.WebSocketEventListener;
import media.toloka.rfa.media.messanger.model.ChatListElement;
import media.toloka.rfa.media.messanger.model.ChatMessage;
import media.toloka.rfa.media.messanger.model.ChatPresenceEvent;
import media.toloka.rfa.media.messanger.model.MessageRoom;
import media.toloka.rfa.media.messanger.model.enumerate.EChatRoomType;
import media.toloka.rfa.media.messanger.service.ChatPresenceService;
import media.toloka.rfa.media.messanger.service.ChatReferenceSingleton;
import media.toloka.rfa.media.messanger.service.ChatRoomPresenceService;
import media.toloka.rfa.media.messanger.service.MessangerService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.*;

import static media.toloka.rfa.media.messanger.model.enumerate.EChatRecordType.RECORD_TYPE_TEXT;

/**
 *  STOMP-контролер для роботи чату.
 * Обробляє повідомлення чату, списки користувачів/кімнат та історію.
 */
@Controller
public class ChatController {

    @Autowired
    private ClientService clientService;

    private SimpMessagingTemplate template;

    @Autowired
    private MessangerService messangerService;

    @Autowired
    private ChatPresenceService presenceService;

    @Autowired
    private ChatRoomPresenceService chatRoomPresenceService;

    @Autowired
    private WebSocketEventListener sessionListener;

    @Autowired
    public ChatController(SimpMessagingTemplate template) {
        this.template = template;
    }

    final Logger logger = LoggerFactory.getLogger(ChatController.class);

    /**
     * Реєстрація сесії та отримання історії публічних/приватних повідомлень.
     * POST /app/hello
     */
    @MessageMapping("/hello")
    public void GetChatMessage(ChatMessage inmsg, @Header("simpSessionId") String sessionId) {
        try {
            // Реєструємо сесію для відстеження відключення
            sessionListener.registerSession(sessionId, inmsg.getUuid());

            Clientdetail cd = clientService.GetClientDetailByUUID(inmsg.getUuid());
            String fullName = cd != null ? cd.getCustname() + " " + cd.getCustsurname() : "Unknown";

            // Публікуємо подію в RabbitMQ
            presenceService.publishPresenceEvent(
                    ChatPresenceEvent.EventType.USER_CONNECTED,
                    inmsg.getUuid(),
                    fullName
            );

            // Історія публічних повідомлень кімнати
            List<ChatMessage> publicMessageList = messangerService.GetChatPublicRoomList(inmsg.getRoomuuid());
            for (ChatMessage imsg : publicMessageList) {
                this.template.convertAndSend("/topic/chat.public." + inmsg.getUuid(), imsg);
            }

            // Історія приватних повідомлень + позначка прочитаного
            List<ChatMessage> privateMessageList = messangerService.GetMessagesAsc(inmsg.getUuid());
            for (ChatMessage imsg : privateMessageList) {
                if (imsg.getReading() != null && !imsg.getReading()) {
                    imsg.setReading(true);
                    imsg.setRead(new Date());
                    messangerService.SaveMessage(imsg);
                }
                this.template.convertAndSend("/topic/chat.private." + inmsg.getUuid(), imsg);
            }
        } catch (Exception e) {
            logger.error("PutChatPullInitMessage Exception", e);
        }
    }

    /**
     * Список всіх онлайн-користувачів (для сайдбара чату).
     * POST /app/userslist
     */
    @MessageMapping("/userslist")
    public void GetRoomUserList(ChatMessage inmsg) {
        try {
            ChatReferenceSingleton chatReference = ChatReferenceSingleton.getInstance();
            ChatMessage cmsg = new ChatMessage();
            List<String> usersListMap = new ArrayList<>();

            Map<String, String> usersMap = chatReference.GetUsersMap();
            Gson gson = new GsonBuilder().create();

            for (Map.Entry<String, String> entry : usersMap.entrySet()) {
                ChatListElement chatListElement = new ChatListElement();
                chatListElement.setUuid(entry.getKey());
                chatListElement.setName(entry.getValue());
                chatListElement.setUnreadCount(messangerService.GetUnreadCount(entry.getKey(), inmsg.getUuid()));
                usersListMap.add(gson.toJson(chatListElement));
            }

            cmsg.setBody(gson.toJson(usersListMap));
            cmsg.setUuid(inmsg.getUuid());
            this.template.convertAndSend("/topic/chat.userslist." + cmsg.getUuid(), cmsg);
        } catch (Exception e) {
            logger.error("GetRoomUserList Exception", e);
        }
        messangerService.CheckUserLastLiveTime();
    }

    /**
     * Список публічних кімнат (для сайдбара чату).
     * POST /app/roomslist
     */
    @MessageMapping("/roomslist")
    public void GetRoomList(ChatMessage inmsg) {
        try {
            ChatReferenceSingleton chatReference = ChatReferenceSingleton.getInstance();
            Map<String, Date> lastLivetime = chatReference.GetUserLastLiveTime();
            lastLivetime.put(inmsg.getUuid(), new Date());

            ChatMessage cmsg = new ChatMessage();
            List<String> roomsListMap = new ArrayList<>();

            Map<String, String> roomList = chatReference.GetRoomsMap();
            if (roomList.isEmpty()) {
                List<MessageRoom> messageRoomList = messangerService.GetChatRoomList();
                for (MessageRoom entry : messageRoomList) {
                    if ((entry.getRoomtype() == EChatRoomType.CHATROOM_TYPE_PUBLIC)
                            || ((entry.getRoomtype() == EChatRoomType.CHATROOM_TYPE_STATION) && entry.getRoomOnlineStatus())) {
                        roomList.put(entry.getUuid(), entry.getRoomname());
                    }
                }
            }

            Gson gson = new GsonBuilder().create();
            for (Map.Entry<String, String> entry : roomList.entrySet()) {
                ChatListElement chatListElement = new ChatListElement();
                chatListElement.setUuid(entry.getKey());
                chatListElement.setName(entry.getValue());
                roomsListMap.add(gson.toJson(chatListElement));
            }

            cmsg.setBody(gson.toJson(roomsListMap));
            cmsg.setUuid(inmsg.getUuid());
            this.template.convertAndSend("/topic/chat.roomslist." + inmsg.getUuid(), cmsg);
        } catch (Exception e) {
            logger.error("GetRoomList Exception", e);
        }
    }

    /**
     * 🎯 НОВИЙ МЕТОД: Отримання списку учасників конкретної кімнати.
     * Повертає тільки тих, хто зараз підписаний на /topic/{roomUuid}.
     * POST /app/room/participants
     * Payload: { "uuid": "user-uuid", "roomuuid": "room-uuid" }
     */
    @MessageMapping("/room/participants")
    public void getRoomParticipants(ChatMessage inmsg) {
        try {
            String roomUuid = inmsg.getRoomuuid();
            if (roomUuid == null || roomUuid.isEmpty()) {
                logger.warn("getRoomParticipants: roomuuid is empty");
                return;
            }

            Set<String> participantUuids = chatRoomPresenceService.getRoomParticipants(roomUuid);

            // Формуємо список з іменами
            List<Map<String, String>> userList = new ArrayList<>();
            for (String userUuid : participantUuids) {
                Map<String, String> user = new HashMap<>();
                user.put("uuid", userUuid);

                Clientdetail cd = clientService.GetClientDetailByUUID(userUuid);
                if (cd != null) {
                    user.put("name", cd.getCustname() + " " + cd.getCustsurname());
                } else {
                    user.put("name", "Unknown");
                }
                userList.add(user);
            }

            // Відправляємо результат назад клієнту
            ChatMessage response = new ChatMessage();
            response.setUuid(inmsg.getUuid());
            response.setRoomuuid(roomUuid);
            response.setBody(new GsonBuilder().create().toJson(userList));

            this.template.convertAndSend("/topic/chat.room.participants." + inmsg.getUuid(), response);

            logger.debug("📋 Учасники кімнати {}: {}", roomUuid, userList.size());
        } catch (Exception e) {
            logger.error("Error getting room participants", e);
        }
    }

    /**
     * Отримання історії повідомлень для конкретної кімнати.
     * POST /app/room/history
     */
    @MessageMapping("/room/history")
    public void getRoomHistory(ChatMessage inmsg) {
        try {
            List<ChatMessage> publicMessageList = messangerService.GetChatPublicRoomList(inmsg.getRoomuuid());
            for (ChatMessage imsg : publicMessageList) {
                this.template.convertAndSend("/topic/chat.public." + inmsg.getUuid(), imsg);
            }
        } catch (Exception e) {
            logger.error("Error fetching room history", e);
        }
    }

    /**
     * Отримання історії приватних повідомлень.
     * POST /app/private/history
     */
    @MessageMapping("/private/history")
    public void getPrivateHistory(ChatMessage inmsg) {
        try {
            messangerService.MarkAsRead(inmsg.getTouuid(), inmsg.getUuid());
            presenceService.publishPresenceEvent(ChatPresenceEvent.EventType.USER_CONNECTED, inmsg.getUuid(), null);

            List<ChatMessage> privateMessageList = messangerService.GetMessagesAsc(inmsg.getUuid());
            for (ChatMessage imsg : privateMessageList) {
                this.template.convertAndSend("/topic/chat.private." + inmsg.getUuid(), imsg);
            }
        } catch (Exception e) {
            logger.error("Error fetching private history", e);
        }
    }

    /**
     * Публічне повідомлення в кімнату.
     * POST /app/public
     */
    @MessageMapping("/public")
    public void GetChatPublicmessage(ChatMessage inmsg) {
        try {
            this.template.convertAndSend("/topic/" + inmsg.getRoomuuid(), inmsg);
        } catch (Exception e) {
            logger.error("PutChatPublicMessage Exception", e);
        }
        inmsg.setUuid(UUID.randomUUID().toString());
        inmsg.setRtype(RECORD_TYPE_TEXT.ordinal());
        messangerService.SaveMessageFromChat(inmsg);
    }

    /**
     * Приватне повідомлення.
     * POST /app/private
     */
    @MessageMapping("/private")
    public void GetChatPrivateMessage(ChatMessage message) {
        try {
            Clientdetail cd = clientService.GetClientDetailByUuid(message.getTouuid());
            message.setSend(new Date());
            if (cd != null) {
                if (message.getTouuid().indexOf(message.getFromuuid()) == -1) {
                    this.template.convertAndSend("/topic/chat.private." + cd.getUuid(), message);
                }
                this.template.convertAndSend("/topic/chat.private." + message.getFromuuid(), message);
            }
        } catch (Exception e) {
            logger.error("PutChatPrivateMessage Exception", e);
        }
        message.setUuid(UUID.randomUUID().toString());
        message.setRtype(RECORD_TYPE_TEXT.ordinal());
        messangerService.SaveMessageFromChat(message);
    }

    /**
     * Сторінка чату.
     */
    @GetMapping(value = "/chat")
    public String getUserHome(Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }
        Clientdetail cd = clientService.GetClientDetailByUser(user);
        List<Clientdetail> userlist = messangerService.FindAllCorrespondentsList(cd);
        model.addAttribute("userlist", userlist);
        model.addAttribute("curuuid", cd.getUuid());
        model.addAttribute("curusername", cd.getCustname() + " " + cd.getCustsurname());
        model.addAttribute("curroom", messangerService.GetChatRoomById(1L).getUuid());
        model.addAttribute("roomlist", messangerService.GetChatRoomList());
        return "/messenger/messenger";
    }
}