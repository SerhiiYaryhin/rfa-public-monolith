package media.toloka.rfa.tetegrambot.handler.impl.createlogin;

import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.tetegrambot.enums.ConversationState;
import media.toloka.rfa.tetegrambot.handler.UserRequestHandler;
import media.toloka.rfa.tetegrambot.helper.KeyboardHelper;
import media.toloka.rfa.tetegrambot.model.UserRequest;
import media.toloka.rfa.tetegrambot.model.UserSession;
import media.toloka.rfa.tetegrambot.service.TelegramService;
import media.toloka.rfa.tetegrambot.service.UserSessionService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import static media.toloka.rfa.tetegrambot.constant.Constants.BTN_TO_RFA_REGISTERS;

@Slf4j
@Profile("Telegram")
@Component
public class LoginS0EnteredHandler extends UserRequestHandler {

    private final TelegramService telegramService;
    private final KeyboardHelper keyboardHelper;
    private final UserSessionService userSessionService;

    public LoginS0EnteredHandler(TelegramService telegramService, KeyboardHelper keyboardHelper, UserSessionService userSessionService) {
        this.telegramService = telegramService;
        this.keyboardHelper = keyboardHelper;
        this.userSessionService = userSessionService;
    }
    public boolean isApplicable(UserRequest userRequest) { return isTextMessage(userRequest.getUpdate(), BTN_TO_RFA_REGISTERS);
//        return isTextMessage(userRequest.getUpdate())
//                && ConversationState.WAITING_CD_FOR_TELEGRAM_LINK_UUID.equals(userRequest.getUserSession().getState());
    }

    @Override
    public void handle(UserRequest userRequest) {
        // Початок діалогу з реєстрації користувача на порталі
        //Світимо повідомлення про необхідність ввести пошту
        UserSession session = userRequest.getUserSession();
        session.setUserId(userRequest.getUserSession().getUserId());
        // Встановлюємо стан для наступного кроку
        session.setState(ConversationState.WAITING_LOGIN_FNAME); //Для наступного кроку
        // Зберігаємо в статусі користувача сеанс та користувача
        userSessionService.saveSession(userRequest.getChatId(), session);
        // Повідомлення про наступний крок
        ReplyKeyboardMarkup replyKeyboardMarkup = keyboardHelper.buildMenuWithCancel();
        telegramService.sendMessage(userRequest.getChatId(),"Будь ласка, введіть Ваше імʼя.",
                replyKeyboardMarkup);
    }

    @Override
    public boolean isGlobal() {
        return true;
    }
}
