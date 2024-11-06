package media.toloka.rfa.tetegrambot.handler.impl.send2chat;
// Надсилаємо повідомлення в чат прямого ефіру
// перелік прямих ефірів беремо з чату
import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.media.messanger.model.MessageRoom;
import media.toloka.rfa.media.messanger.service.MessangerService;
import media.toloka.rfa.tetegrambot.enums.ConversationState;
import media.toloka.rfa.tetegrambot.handler.UserRequestHandler;
import media.toloka.rfa.tetegrambot.helper.KeyboardHelper;
import media.toloka.rfa.tetegrambot.model.UserRequest;
import media.toloka.rfa.tetegrambot.model.UserSession;
import media.toloka.rfa.tetegrambot.service.TelegramService;
import media.toloka.rfa.tetegrambot.service.UserSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.util.List;

import static media.toloka.rfa.tetegrambot.constant.Constants.BTN_SEND_TO_CHAT;

@Slf4j
@Component
public class Step_2_GetChatID extends UserRequestHandler {

    @Autowired
    private MessangerService messangerService;

    private final TelegramService telegramService;
    private final KeyboardHelper keyboardHelper;
    private final UserSessionService userSessionService;



    public Step_2_GetChatID(TelegramService telegramService, KeyboardHelper keyboardHelper, UserSessionService userSessionService) {
        this.telegramService = telegramService;
        this.keyboardHelper = keyboardHelper;
        this.userSessionService = userSessionService;
    }

    @Override
    public boolean isGlobal() {
        return false;
    }
    @Override
    public boolean isApplicable(UserRequest userRequest) {
//        return false;
//        return isTextMessage(userRequest.getUpdate(), BTN_SEND_TO_CHAT);
//        return (isAudioMessage(userRequest.getUpdate()) || isDocumentMessage(userRequest.getUpdate()))
        return isTextMessage(userRequest.getUpdate())
                && ConversationState.WAITING_CHAT_CHOOSE.equals(userRequest.getUserSession().getState());
    }

    @Override
    public void handle(UserRequest userRequest) {
        ReplyKeyboardMarkup replyKeyboardMarkup = keyboardHelper.buildMenuWithCancel();
        // Отримуємо ID чата в який будемо надсилати повідомлення.
        String stringGetChat = userRequest.getUpdate().getMessage().getText();
        long chatID;
        try {
            chatID = Long.parseLong(stringGetChat);
        } catch (NumberFormatException e) {
            log.info("Проблеми при перетворенні отриманого рядка в Long.");
            replyKeyboardMarkup = keyboardHelper.buildMenuWithCancel();
            telegramService.sendMessage(userRequest.getChatId(),
                    "Йой! Щось пішло не так :( \nЗнову оберіть активний чат та введіть його номер.",
                    replyKeyboardMarkup);
            // формуємо перелік чатів і нумеруємо їх
            String stringListRoomsOnline = "";
            List<MessageRoom> listRoom = messangerService.GetChatRoomList();
            for (MessageRoom p : listRoom) {
                if (p.getRoomOnlineStatus()) {
                    stringListRoomsOnline += "\n"+ p.getId().toString()+ " - "+ p.getRoomname();
                }
            }
            telegramService.sendMessage(userRequest.getChatId(),
                    stringListRoomsOnline);
            // візначаємо наступний крок
            UserSession session = userRequest.getUserSession();
            session.setState(ConversationState.WAITING_CHAT_CHOOSE);
            userSessionService.saveSession(userRequest.getChatId(), session);
            return;
        }
        // Якась херня :(
        MessageRoom curRoom = messangerService.GetChatRoomById(chatID);
        MessageRoom cr2 = messangerService.GetRoomNameByUuid(curRoom.getUuid());
        userRequest.getUserSession().setRFA_Chat_Choose(cr2);
        // візначаємо наступний крок
        telegramService.sendMessage(userRequest.getChatId(),
                "Введіть повідомлення та надішліть\nДля завершення натисніть \"Скасувати\"",
                replyKeyboardMarkup);
        UserSession session = userRequest.getUserSession();
        session.setState(ConversationState.WAITING_CHAT_MESSAGE);
        userSessionService.saveSession(userRequest.getChatId(), session);
        return;
    }
}
