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

@Slf4j
@Profile("Telegram")
@Component
public class LoginS5GetEmailEnteredHandler extends UserRequestHandler {

    private final TelegramService telegramService;
    private final KeyboardHelper keyboardHelper;
    private final UserSessionService userSessionService;

    public LoginS5GetEmailEnteredHandler(TelegramService telegramService, KeyboardHelper keyboardHelper, UserSessionService userSessionService) {
        this.telegramService = telegramService;
        this.keyboardHelper = keyboardHelper;
        this.userSessionService = userSessionService;
    }

    public boolean isApplicable(UserRequest userRequest) {
//        return isTextMessage(userRequest.getUpdate(), "ееее"+BTN_TO_RFA_REGISTERS);
        return isTextMessage(userRequest.getUpdate())
                && ConversationState.WAITING_LOGIN_EMAIL.equals(userRequest.getUserSession().getState());
    }

    @Override
    public void handle(UserRequest userRequest) {
        UserSession session = userRequest.getUserSession();
        session.setUserEmail (userRequest.getUpdate().getMessage().getText());
        // Встановлюємо стан для наступного кроку
        session.setState(ConversationState.WAITING_LOGIN_CHECK_REPLY); //Для наступного кроку
        // Зберігаємо в статусі користувача сеанс та користувача
        userSessionService.saveSession(userRequest.getChatId(), session);
        // Повідомлення про наступний крок
        telegramService.sendMessage(userRequest.getChatId(),
                "Ви ввели:"
                        +"\nІмʼя: "+session.getUserFName()
                        +"\nПризвище: "+session.getUserSName()
                        +"\nПошта: "+session.getUserEmail()
        );

        ReplyKeyboardMarkup replyKeyboardMarkup = keyboardHelper.buildYesNoCancelMenu();
        telegramService.sendMessage(userRequest.getChatId(),"Все правильно?",
                replyKeyboardMarkup);


    }

    @Override
    public boolean isGlobal() {
        return false;
    }
}
