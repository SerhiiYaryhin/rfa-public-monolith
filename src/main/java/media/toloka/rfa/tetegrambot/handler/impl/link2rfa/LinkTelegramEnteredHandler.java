package media.toloka.rfa.tetegrambot.handler.impl.link2rfa;


import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.tetegrambot.enums.ConversationState;
import media.toloka.rfa.tetegrambot.handler.UserRequestHandler;
import media.toloka.rfa.tetegrambot.helper.KeyboardHelper;
import media.toloka.rfa.tetegrambot.model.UserRequest;
import media.toloka.rfa.tetegrambot.model.UserSession;
import media.toloka.rfa.tetegrambot.service.TelegramService;
import media.toloka.rfa.tetegrambot.service.UserSessionService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import static media.toloka.rfa.tetegrambot.constant.Constants.BTN_TELEGRAM_LINK;

@Slf4j
@Component
public class LinkTelegramEnteredHandler extends UserRequestHandler {

    private final TelegramService telegramService;
    private final KeyboardHelper keyboardHelper;
    private final UserSessionService userSessionService;

    public LinkTelegramEnteredHandler(TelegramService telegramService, KeyboardHelper keyboardHelper, UserSessionService userSessionService) {
        this.telegramService = telegramService;
        this.keyboardHelper = keyboardHelper;
        this.userSessionService = userSessionService;
    }

    @Override
    public boolean isApplicable(UserRequest userRequest) { return isTextMessage(userRequest.getUpdate(), BTN_TELEGRAM_LINK);
//        return isTextMessage(userRequest.getUpdate())
//                && ConversationState.WAITING_CD_FOR_TELEGRAM_LINK_UUID.equals(userRequest.getUserSession().getState());
    }

    @Override
    public void handle(UserRequest userRequest) {
        ReplyKeyboardMarkup replyKeyboardMarkup = keyboardHelper.buildMenuWithCancel();
//        telegramService.sendMessage(userRequest.getChatId(),
//                "✍️Тепер ми спробуємо привʼязати Ваш Телеграм та обліковий запис на порталі️");
        telegramService.sendMessage(userRequest.getChatId(),
                "✍️Тепер ми спробуємо привʼязати Ваш Телеграм та обліковий запис на порталі️. "
                        +"Будь ласка, скопіюйте та відправте ідентифікатор, який Ви можете отримати у Вашому профайлі.",
                replyKeyboardMarkup);


        String RFA_Telegram_UUID = userRequest.getUpdate().getMessage().getText();
//        log.info("RFA_Telegram_UUID: ",RFA_Telegram_UUID);
        UserSession session = userRequest.getUserSession();
//        session.setCity(city);
        session.setState(ConversationState.WAITING_CD_FOR_TELEGRAM_LINK_UUID);
        userSessionService.saveSession(userRequest.getChatId(), session);
    }

    @Override
    public boolean isGlobal() {
        return false;
    }

}
