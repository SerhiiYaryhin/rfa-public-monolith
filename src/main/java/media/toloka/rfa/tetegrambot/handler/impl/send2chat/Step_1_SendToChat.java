package media.toloka.rfa.tetegrambot.handler.impl.send2chat;
// Надсилаємо повідомлення в чат прямого ефіру
// перелік прямих ефірів беремо з чату
import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.media.messanger.model.MessageRoom;
import media.toloka.rfa.media.messanger.service.MessangerService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.tetegrambot.enums.ConversationState;
import media.toloka.rfa.tetegrambot.handler.UserRequestHandler;
import media.toloka.rfa.tetegrambot.helper.KeyboardHelper;
import media.toloka.rfa.tetegrambot.model.UserRequest;
import media.toloka.rfa.tetegrambot.model.UserSession;
import media.toloka.rfa.tetegrambot.service.TelegramService;
import media.toloka.rfa.tetegrambot.service.UserSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.util.List;

import static media.toloka.rfa.tetegrambot.constant.Constants.BTN_SEND_TO_CHAT;

@Slf4j
@Component
public class Step_1_SendToChat extends UserRequestHandler {

    @Autowired
    MessangerService messangerService;
    @Autowired
    private ClientService clientService;

    private final TelegramService telegramService;
    private final KeyboardHelper keyboardHelper;
    private final UserSessionService userSessionService;

    public Step_1_SendToChat(TelegramService telegramService, KeyboardHelper keyboardHelper, UserSessionService userSessionService) {
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
        return isTextMessage(userRequest.getUpdate(), BTN_SEND_TO_CHAT);
//        return (isAudioMessage(userRequest.getUpdate()) || isDocumentMessage(userRequest.getUpdate()))
//        return isAudioMessage(userRequest.getUpdate())
//                && ConversationState.WAITING_SEND_TRACK.equals(userRequest.getUserSession().getState());
    }

    @Override
    public void handle(UserRequest userRequest) {
        ReplyKeyboardMarkup replyKeyboardMarkup = keyboardHelper.buildMenuWithCancel();
//        telegramService.sendMessage(userRequest.getChatId(),
//                "Оберіть активний чат та введіть його номер.",
//                replyKeyboardMarkup);
        // формуємо перелік чатів і нумеруємо їх
        String stringListRoomsOnline = "";
        Boolean isOnline = false;
        List<MessageRoom> listRoom = messangerService.GetChatRoomList();
        for (MessageRoom p : listRoom) {
            if (p.getRoomOnlineStatus()) {
                stringListRoomsOnline += "\n"+ p.getId().toString()+ " - "+ p.getRoomname();
                isOnline = true;
            }
        }
        if (isOnline) {
            telegramService.sendMessage(userRequest.getChatId(),
                    "Оберіть активний чат та введіть його номер.",
                    replyKeyboardMarkup);
            telegramService.sendMessage(userRequest.getChatId(),
                    stringListRoomsOnline);
        } else {
            telegramService.sendMessage(userRequest.getChatId(),
                    "На цей час немає чатів в прямому ефірі.");
            Clientdetail cd =  clientService.GetUserFromTelegram(userRequest.getUpdate().getMessage().getFrom().getId().toString());
            if (cd != null) {
                ReplyKeyboard replyKeyboard = keyboardHelper.buildMainMenu();
                telegramService.sendMessage(userRequest.getChatId(),
                        "\uD83D\uDC4BПривіт! За допомогою цього чат-бота ви зможете надіслати свої треки на портал \"Радіо для всіх!\"",
                        replyKeyboard);
            }
            else {
                ReplyKeyboard replyKeyboard = keyboardHelper.buildRegisterMenu();
                telegramService.sendMessage(userRequest.getChatId(),
                        "\uD83D\uDC4BПривіт! За допомогою цього чат-бота ви зможете надіслати свої треки на портал \"Радіо для всіх!\"",
                        replyKeyboard);
                telegramService.sendMessage(userRequest.getChatId(),
                        "Для цього Ви повинні бути зареєстровані на порталі https://rfa.toloka.media/ "
                                +"та привʼязати свій Телеграм до облікового запису на порталі.");

            }

            UserSession userSession = userRequest.getUserSession();
            userSession.setState(ConversationState.CONVERSATION_STARTED);
            userSessionService.saveSession(userSession.getChatId(), userSession);
            return;
        }
        // візначаємо наступний крок
        UserSession session = userRequest.getUserSession();
        session.setState(ConversationState.WAITING_CHAT_CHOOSE);
        userSessionService.saveSession(userRequest.getChatId(), session);
    }
}
