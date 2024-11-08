package media.toloka.rfa.media.messanger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import media.toloka.rfa.media.messanger.model.enumerate.EChatRoomType;
import media.toloka.rfa.media.messanger.service.ChatReferenceSingleton;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.media.messanger.model.ChatListElement;
import media.toloka.rfa.media.messanger.model.ChatMessage;
import media.toloka.rfa.media.messanger.service.MessangerService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.media.messanger.model.MessageRoom;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.*;

import static media.toloka.rfa.media.messanger.model.enumerate.EChatRecordType.RECORD_TYPE_TEXT;


@Controller
public class ChatController {
// https://kiberstender.github.io/miscelaneous-spring-websocket-stomp-specific-user/
// https://medium.com/@liakh-aliaksandr/websockets-with-spring-part-3-stomp-over-websocket-3dab4a21f397
// https://stomp-js.github.io/guide/stompjs/using-stompjs-v5.html

    @Autowired
    private ClientService clientService;

    private SimpMessagingTemplate template;

    @Autowired
    private MessangerService messangerService;

    @Autowired
    public ChatController(SimpMessagingTemplate template) {
        this.template = template;
    }

    final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @MessageMapping("/hello")
    public void GetChatMessage( ChatMessage inmsg) {
            // add user to userlist for room
            try {
            messangerService.AddChatUser(inmsg);
            Clientdetail cd = clientService.GetClientDetailByUUID(inmsg.getUuid());

            List<ChatMessage> publicMessageList = messangerService.GetChatPublicRoomList(inmsg.getRoomuuid());
            ChatMessage cmsg = new ChatMessage();
            Iterator<ChatMessage> iterator = publicMessageList.iterator();
            while (iterator.hasNext()) {
                ChatMessage imsg = iterator.next();
                this.template.convertAndSend("/public/"+inmsg.getUuid(), imsg);
            }
            List<ChatMessage> privateMessageList = messangerService.GetMessagesAsc(inmsg.getUuid());
            Iterator<ChatMessage> iteratorp = privateMessageList.iterator();
            while (iteratorp.hasNext()) {
                ChatMessage imsg = iteratorp.next();
                if (imsg.getReading() != null) {
                    if (imsg.getReading() != true) {
                        imsg.setReading(true);
                        imsg.setRead(new Date());
                        messangerService.SaveMessage(imsg);
                    }
                }
                this.template.convertAndSend("/private/"+inmsg.getUuid(), imsg);
            }
        } catch (Exception e) {
        logger.info("PutChatPullInitMessage Exception");
                e.printStackTrace();
    }
        }

    @MessageMapping("/userslist")
    public void GetRoomUserList( ChatMessage inmsg) {
//        logger.info("Чат. GetRoomUserList inmsg getUuid: {} getRoomuuid(): {}",inmsg.getUuid(),inmsg.getRoomuuid());
        // add user to userlist for room
        try {
            // get Instance ChatReferenceSingleton
            ChatReferenceSingleton chatReference = ChatReferenceSingleton.getInstance();
            ChatMessage cmsg = new ChatMessage();
            List<String > usersListMap =  new ArrayList<>();

            Map<String, String> usersMap =  chatReference.GetUsersMap();
            Gson gson = new GsonBuilder().create();
            ChatListElement chatListElement = new ChatListElement();
            for (Map.Entry<String, String> entry : usersMap.entrySet()) {
                chatListElement.setUuid(entry.getKey());
                chatListElement.setName(entry.getValue());
                String jcl = gson.toJson(chatListElement);
                usersListMap.add(jcl);
//                System.out.println(entry.getKey() + ":" + entry.getValue());
            }
            String json = gson.toJson(usersListMap);
//            System.out.println(json);
            cmsg.setBody(json);
            cmsg.setUuid(inmsg.getUuid());
            this.template.convertAndSend("/userslist/"+cmsg.getUuid(), cmsg);
        } catch (Exception e) {
            logger.info("GetRoomUserList Exception");
            e.printStackTrace();
        }
        messangerService.CheckUserLastLiveTime();
    }

    @MessageMapping("/roomslist")
    public void GetRoomList( ChatMessage inmsg) {
//        logger.info("Чат. GetRoomList inmsg getUuid: {} getRoomuuid(): {}",inmsg.getUuid(),inmsg.getRoomuuid());
        // add user to userlist for room
        try {
            // get Instance ChatReferenceSingleton
            ChatReferenceSingleton chatReference = ChatReferenceSingleton.getInstance();
            Map<String, Date> lastLivetime = chatReference.GetUserLastLiveTime();
            lastLivetime.put(inmsg.getUuid(), new Date());
            ChatMessage cmsg = new ChatMessage();
            List<String > usersListMap =  new ArrayList<>();

            Map<String, String> roomList =  chatReference.GetRoomsMap();
            roomList.clear();

            if (roomList.size() == 0) {
                // Init list rooms
                List<MessageRoom> messageRoomList = messangerService.GetChatRoomList();
                for (MessageRoom entry : messageRoomList) {
                    // Вибираємо Публічні кімнати і кімнати радіостанцій в онлайні
                    if ((entry.getRoomtype() == EChatRoomType.CHATROOM_TYPE_PUBLIC) || ((entry.getRoomtype() == EChatRoomType.CHATROOM_TYPE_STATION) && entry.getRoomOnlineStatus() ) ) {
                        roomList.put(entry.getUuid(), entry.getRoomname());
                    }
                }
            }
            Gson gson = new GsonBuilder().create();
            ChatListElement chatListElement = new ChatListElement();
            for (Map.Entry<String, String> entry : roomList.entrySet()) {
                chatListElement.setUuid(entry.getKey());
                chatListElement.setName(entry.getValue());
                String jcl = gson.toJson(chatListElement);
                usersListMap.add(jcl);
//                System.out.println(entry.getKey() + ":" + entry.getValue());
            }
            String json = gson.toJson(usersListMap);
//            System.out.println(json);
            cmsg.setBody(json);
            cmsg.setUuid(inmsg.getUuid());
            this.template.convertAndSend("/roomslist/"+inmsg.getUuid(), cmsg);
        } catch (Exception e) {
            logger.info("GetRoomUserList Exception");
            e.printStackTrace();
        }
    }

    @MessageMapping("/public")
    public void GetChatPublicmessage( ChatMessage inmsg) {
        try {
            this.template.convertAndSend("/topic/"+inmsg.getRoomuuid(), inmsg);
        } catch (Exception e) {
            logger.info("PutChatPrivateMessage Exception");
            e.printStackTrace();
        }
        inmsg.setRtype(RECORD_TYPE_TEXT.ordinal());
        messangerService.SaveMessageFromChat(inmsg);
    }

    @MessageMapping("/private")
    public void GetChatPrivateMessage( ChatMessage message) {
        try {
            Clientdetail cd = clientService.GetClientDetailByUuid(message.getTouuid());
            message.setSend(new Date());
            if (cd != null) {
                if (message.getTouuid().indexOf(message.getFromuuid()) == -1) {
                    this.template.convertAndSend("/private/"+cd.getUuid(), message); // TO
                }
                this.template.convertAndSend("/private/"+message.getFromuuid(), message); // FROM
            }
        } catch (Exception e) {
            logger.info("PutChatPrivateMessage Exception");
            e.printStackTrace();
        }
        message.setRtype(RECORD_TYPE_TEXT.ordinal());
        messangerService.SaveMessageFromChat(message);
    }

    @GetMapping(value = "/chat")
    public String getUserHome(
            Model model ) {
        Users user = clientService.GetCurrentUser();
        if (user == null) {
            return "redirect:/";
        }

        Clientdetail cd = clientService.GetClientDetailByUser(user);

        List<Clientdetail> userlist = messangerService.FindAllCorrespondentsList(cd);

        model.addAttribute("userlist", userlist );
        model.addAttribute("curuuid", cd.getUuid() );
        model.addAttribute("curusername", cd.getCustname()+" "+cd.getCustsurname() );
        model.addAttribute("curroom", messangerService.GetChatRoomById(1L).getUuid() );
        model.addAttribute("roomlist", messangerService.GetChatRoomList() );
//
        return "/messenger/messenger";
    }
// END
}
