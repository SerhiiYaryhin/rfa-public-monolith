package media.toloka.rfa.tetegrambot.handler.impl.createlogin;


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

@Slf4j
@Component
public class LoginS3GetFirmNameEnteredHandler extends UserRequestHandler {

    private final TelegramService telegramService;
    private final KeyboardHelper keyboardHelper;
    private final UserSessionService userSessionService;

    public LoginS3GetFirmNameEnteredHandler(TelegramService telegramService, KeyboardHelper keyboardHelper, UserSessionService userSessionService) {
        this.telegramService = telegramService;
        this.keyboardHelper = keyboardHelper;
        this.userSessionService = userSessionService;
    }



    public boolean isApplicable(UserRequest userRequest) {
//        return isTextMessage(userRequest.getUpdate(), "ееее"+BTN_TO_RFA_REGISTERS);
        return false;
//        return isTextMessage(userRequest.getUpdate())
//                && ConversationState.WAITING_LOGIN_FIRMNAME.equals(userRequest.getUserSession().getState());
//                //todo перевірити чи цей користувач знаходиться у цьому стані
//                && userRequest.getUserSession().getUserId().equals(userRequest.getUserSession().getUserId())
//        );
    }

    @Override
    public void handle(UserRequest userRequest) {
        //Приймаємо імʼя та зберігаємо його в сессії

        String RFA_Telegram_UUID = userRequest.getUpdate().getMessage().getText();

        UserSession session = userRequest.getUserSession();
        session.setUserFirmName (userRequest.getUpdate().getMessage().getText());
        // Встановлюємо стан для наступного кроку
        session.setState(ConversationState.WAITING_LOGIN_PASSWORD); //Для наступного кроку
        // Зберігаємо в статусі користувача сеанс та користувача
        userSessionService.saveSession(userRequest.getChatId(), session);
        // Повідомлення про наступний крок
        ReplyKeyboardMarkup replyKeyboardMarkup = keyboardHelper.buildMenuWithCancel();
        telegramService.sendMessage(userRequest.getChatId(),"Будь ласка, введіть ваш пароль для входу на портал.",
                replyKeyboardMarkup);


    }

    @Override
    public boolean isGlobal() {
        return false;
    }
}
