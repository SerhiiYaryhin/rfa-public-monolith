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
import media.toloka.rfa.media.messanger.service.HandRaiseService;
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
    private WebSocketEventListener sessionListener;
    @Autowired
    private ChatRoomPresenceService chatRoomPresenceService;
    @Autowired
    private HandRaiseService handRaiseService;

    @Autowired
    public ChatController(SimpMessagingTemplate template) {
        this.template = template;
    }

    final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @MessageMapping("/hello")
    public void GetChatMessage(ChatMessage inmsg, @Header("simpSessionId") String sessionId) {
        try {
            sessionListener.registerSession(sessionId, inmsg.getUuid());
            Clientdetail cd = clientService.GetClientDetailByUUID(inmsg.getUuid());
            String fullName = cd != null ? cd.getCustname() + " " + cd.getCustsurname() : "Unknown";

            // Заміна email на UUID, якщо інтерсептор ще не знав UUID
            if (cd != null && cd.getUser() != null) {
                String email = cd.getUser().getEmail();
                chatRoomPresenceService.replaceEmailWithUuid(email, inmsg.getUuid());
            }

            presenceService.publishPresenceEvent(ChatPresenceEvent.EventType.USER_CONNECTED, inmsg.getUuid(), fullName);

            List<ChatMessage> publicMessageList = messangerService.GetChatPublicRoomList(inmsg.getRoomuuid());
            for (ChatMessage imsg : publicMessageList) {
                this.template.convertAndSend("/topic/chat.public." + inmsg.getUuid(), imsg);
            }
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

    @MessageMapping("/room/participants")
    public void getRoomParticipants(ChatMessage inmsg) {
        try {
            String roomUuid = inmsg.getRoomuuid();
            if (roomUuid == null || roomUuid.isEmpty()) return;
            Set<String> participantUuids = chatRoomPresenceService.getRoomParticipants(roomUuid);
            List<Map<String, Object>> userList = new ArrayList<>();
            for (String userUuid : participantUuids) {
                Map<String, Object> user = new HashMap<>();
                user.put("uuid", userUuid);
                user.put("muted", chatRoomPresenceService.isMuted(userUuid));
                Clientdetail cd = clientService.GetClientDetailByUuid(userUuid);
                if (cd == null) cd = clientService.GetClientDetailByUUID(userUuid);
                user.put("name", cd != null ? cd.getCustname() + " " + cd.getCustsurname()
                        : "Unknown (" + userUuid.substring(0, Math.min(8, userUuid.length())) + "...)");
                userList.add(user);
            }
            ChatMessage response = new ChatMessage();
            response.setUuid(inmsg.getUuid());
            response.setRoomuuid(roomUuid);
            response.setBody(new GsonBuilder().create().toJson(userList));
            this.template.convertAndSend("/topic/chat.room.participants." + inmsg.getUuid(), response);
        } catch (Exception e) {
            logger.error("Error getting room participants", e);
        }
    }

    @MessageMapping("/rooms/batch-participants")
    public void getBatchRoomParticipants(ChatMessage inmsg) {
        try {
            String bodyJson = inmsg.getBody();
            if (bodyJson == null || bodyJson.isEmpty()) return;
            com.google.gson.JsonObject request = com.google.gson.JsonParser.parseString(bodyJson).getAsJsonObject();
            com.google.gson.JsonArray roomUuidsArray = request.getAsJsonArray("roomUuids");
            if (roomUuidsArray == null) return;

            Map<String, List<Map<String, Object>>> result = new HashMap<>();
            for (com.google.gson.JsonElement el : roomUuidsArray) {
                String roomUuid = el.getAsString();
                if (roomUuid == null || roomUuid.isEmpty()) continue;
                Set<String> participantUuids = chatRoomPresenceService.getRoomParticipants(roomUuid);
                List<Map<String, Object>> userList = new ArrayList<>();
                for (String userUuid : participantUuids) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("uuid", userUuid);
                    user.put("muted", chatRoomPresenceService.isMuted(userUuid));
                    Clientdetail cd = clientService.GetClientDetailByUuid(userUuid);
                    if (cd == null) cd = clientService.GetClientDetailByUUID(userUuid);
                    user.put("name", cd != null ? cd.getCustname() + " " + cd.getCustsurname()
                            : "Unknown (" + userUuid.substring(0, Math.min(8, userUuid.length())) + "...)");
                    userList.add(user);
                }
                result.put(roomUuid, userList);
            }
            ChatMessage response = new ChatMessage();
            response.setUuid(inmsg.getUuid());
            response.setBody(new GsonBuilder().create().toJson(result));
            this.template.convertAndSend("/topic/chat.rooms.batch." + inmsg.getUuid(), response);
        } catch (Exception e) {
            logger.error("Error getting batch room participants", e);
        }
    }

    // ================ ПЕРЕМІЩЕННЯ КОРИСТУВАЧА ================
    @MessageMapping("/room/move")
    public void moveUserToRoom(ChatMessage inmsg) {
        try {
            String targetRoomUuid = inmsg.getRoomuuid();
            if (targetRoomUuid == null || targetRoomUuid.isEmpty()) return;
            com.google.gson.JsonObject body = com.google.gson.JsonParser.parseString(inmsg.getBody()).getAsJsonObject();
            String targetUserUuid = body.get("targetUserUuid").getAsString();
            if (targetUserUuid == null || targetUserUuid.isEmpty()) return;

            chatRoomPresenceService.moveUserToRoom(targetUserUuid, targetRoomUuid);

            Map<String, Object> controlPayload = new HashMap<>();
            controlPayload.put("action", "MOVE_YOURSELF");
            controlPayload.put("targetRoomUuid", targetRoomUuid);
            controlPayload.put("targetUserUuid", targetUserUuid);
            controlPayload.put("autoMute", body.has("autoMute") && body.get("autoMute").getAsBoolean());
            this.template.convertAndSend("/topic/efir.control", controlPayload);

            logger.info("🎛️ Модератор {} перемістив користувача {} в кімнату {}",
                    inmsg.getUuid(), targetUserUuid, targetRoomUuid);
        } catch (Exception e) {
            logger.error("Error moving user to room", e);
        }
    }

    // ================ MUTE / UNMUTE ================
    @MessageMapping("/room/mute")
    public void muteUser(ChatMessage inmsg) {
        try {
            com.google.gson.JsonObject body = com.google.gson.JsonParser.parseString(inmsg.getBody()).getAsJsonObject();
            String targetUserUuid = body.get("targetUserUuid").getAsString();
            boolean muted = body.get("muted").getAsBoolean();
            chatRoomPresenceService.setMuted(targetUserUuid, muted);

            Map<String, Object> controlPayload = new HashMap<>();
            controlPayload.put("action", muted ? "MUTE_YOURSELF" : "UNMUTE_YOURSELF");
            controlPayload.put("targetUserUuid", targetUserUuid);
            this.template.convertAndSend("/topic/efir.control", controlPayload);
        } catch (Exception e) {
            logger.error("Error muting user", e);
        }
    }

    @MessageMapping("/room/mute-all")
    public void muteAll(ChatMessage inmsg) {
        try {
            String roomUuid = inmsg.getRoomuuid();
            if (roomUuid == null || roomUuid.isEmpty()) return;
            com.google.gson.JsonObject body = com.google.gson.JsonParser.parseString(inmsg.getBody()).getAsJsonObject();
            boolean muted = body.get("muted").getAsBoolean();
            if (muted) chatRoomPresenceService.muteAllInRoom(roomUuid);
            else chatRoomPresenceService.unmuteAllInRoom(roomUuid);

            Map<String, Object> controlPayload = new HashMap<>();
            controlPayload.put("action", muted ? "MUTE_ALL_IN_ROOM" : "UNMUTE_ALL_IN_ROOM");
            controlPayload.put("roomUuid", roomUuid);
            this.template.convertAndSend("/topic/efir.control", controlPayload);
        } catch (Exception e) {
            logger.error("Error in mute-all", e);
        }
    }

    // ================ ПІДНЯТТЯ РУКИ ================
    @MessageMapping("/hand/raise")
    public void raiseHand(ChatMessage inmsg) {
        try {
            com.google.gson.JsonObject body = com.google.gson.JsonParser.parseString(inmsg.getBody()).getAsJsonObject();
            String stationUuid = body.get("stationUuid").getAsString();
            String roomUuid = body.get("roomuuid").getAsString();
            handRaiseService.raiseHand(stationUuid, inmsg.getUuid(), roomUuid);
        } catch (Exception e) {
            logger.error("Error raising hand", e);
        }
    }

    @MessageMapping("/hand/lower")
    public void lowerHand(ChatMessage inmsg) {
        try {
            com.google.gson.JsonObject body = com.google.gson.JsonParser.parseString(inmsg.getBody()).getAsJsonObject();
            String stationUuid = body.get("stationUuid").getAsString();
            String targetUuid = body.has("targetUuid") ? body.get("targetUuid").getAsString() : inmsg.getUuid();
            String reason = body.has("reason") ? body.get("reason").getAsString() : "BY_USER";

            HandRaiseService.HandStatus status;
            try {
                status = HandRaiseService.HandStatus.valueOf(reason);
            } catch (IllegalArgumentException e) {
                status = HandRaiseService.HandStatus.BY_USER;
            }
            handRaiseService.lowerHand(stationUuid, targetUuid, status);
        } catch (Exception e) {
            logger.error("Error lowering hand", e);
        }
    }

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

    @GetMapping(value = "/chat")
    public String getUserHome(Model model) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return "redirect:/";
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