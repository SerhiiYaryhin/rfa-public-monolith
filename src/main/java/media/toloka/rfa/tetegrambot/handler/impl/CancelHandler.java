package media.toloka.rfa.tetegrambot.handler.impl;

import lombok.extern.slf4j.Slf4j;
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
import static media.toloka.rfa.tetegrambot.constant.Constants.BTN_CANCEL;

@Slf4j
@Component
public class CancelHandler extends UserRequestHandler {

    @Autowired
    private ClientService clientService;

    private final TelegramService telegramService;
    private final KeyboardHelper keyboardHelper;
    private final UserSessionService userSessionService;

    public CancelHandler(TelegramService telegramService, KeyboardHelper keyboardHelper, UserSessionService userSessionService) {
        this.telegramService = telegramService;
        this.keyboardHelper = keyboardHelper;
        this.userSessionService = userSessionService;

    }

    @Override
    public boolean isApplicable(UserRequest userRequest) {
        return isTextMessage(userRequest.getUpdate(), BTN_CANCEL);
    }

    @Override
    public void handle(UserRequest userRequest) {
        log.info("Cancel start");
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

//        ReplyKeyboard replyKeyboard = keyboardHelper.buildMainMenu();
//        telegramService.sendMessage(userRequest.getChatId(),
//                "Обирайте з меню нижче ⤵️", replyKeyboard);

        UserSession userSession = userRequest.getUserSession();
        userSession.setState(ConversationState.CONVERSATION_STARTED);
        userSessionService.saveSession(userSession.getChatId(), userSession);
        log.info("Cancel start");
    }

    @Override
    public boolean isGlobal() {
        return true;
    }
}
