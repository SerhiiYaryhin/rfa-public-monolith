package media.toloka.rfa.tetegrambot.handler.impl.createlogin;

import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.radio.client.service.ClientService;
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

import static media.toloka.rfa.tetegrambot.constant.Constants.BTN_SEND_YES;
import static media.toloka.rfa.tetegrambot.constant.Constants.BTN_TO_RFA_REGISTERS;

@Slf4j
@Component
public class LoginS7CheckReplyEnteredHandler extends UserRequestHandler {

    @Autowired
    private ClientService clientService;

    private final TelegramService telegramService;
    private final KeyboardHelper keyboardHelper;
    private final UserSessionService userSessionService;

    public LoginS7CheckReplyEnteredHandler(TelegramService telegramService, KeyboardHelper keyboardHelper, UserSessionService userSessionService) {
        this.telegramService = telegramService;
        this.keyboardHelper = keyboardHelper;
        this.userSessionService = userSessionService;
    }
    public boolean isApplicable(UserRequest userRequest) {
//        return isTextMessage(userRequest.getUpdate(), BTN_TO_RFA_REGISTERS);
        return isTextMessage(userRequest.getUpdate())
                && ConversationState.WAITING_LOGIN_CHECK_REPLY.equals(userRequest.getUserSession().getState());
    }

    @Override
    public void handle(UserRequest userRequest) {

        UserSession session = userRequest.getUserSession();
        session.setUserId(userRequest.getUserSession().getUserId());
        if (userRequest.getUpdate().getMessage().getText().equals(BTN_SEND_YES)) {
            // Вся Інформація корректна
            telegramService.sendMessage(userRequest.getChatId(),
                    "Все правильно. \nВідправляємо Вам листа для встановлення паролю.");
        } else {
            // помилка в інформації
            telegramService.sendMessage(userRequest.getChatId(),
                    "Помилка. \nПочинаємо з початку.");
        }

        // Встановлюємо стан для наступного кроку
        session.setState(ConversationState.CONVERSATION_STARTED);
        // Зберігаємо в статусі користувача сеанс та користувача
        userSessionService.saveSession(userRequest.getChatId(), session);
        // Повідомлення про наступний крок
        if (clientService.GetUserFromTelegram(userRequest.getUpdate().getMessage().getFrom().getId().toString()) != null) {
            ReplyKeyboard replyKeyboard = keyboardHelper.buildMainMenu();
            telegramService.sendMessage(userRequest.getChatId(),
                    "\uD83D\uDC4BПривіт! За допомогою цього чат-бота ви зможете взаємодіяти з порталом \"Радіо для всіх!\""
                            +" надіслати свої треки, додати опис треку, надіслати повідомлення в прямий ефір Студії Толока і багато всього іншого.",
                    replyKeyboard);
        }
        else {
            ReplyKeyboard replyKeyboard = keyboardHelper.buildRegisterMenu();
            telegramService.sendMessage(userRequest.getChatId(),
                    "\uD83D\uDC4BПривіт! За допомогою цього чат-бота ви зможете взаємодіяти з порталом \"Радіо для всіх!\""
                            +" надіслати свої треки, додати опис треку, надіслати повідомлення в прямий ефір Студії Толока і багато всього іншого.",
                    replyKeyboard);
            telegramService.sendMessage(userRequest.getChatId(),
                    "Для цього Ви повинні бути зареєстровані на порталі https://rfa.toloka.media/ "
                            +"та привʼязати свій Телеграм до облікового запису на порталі.");

        }

        telegramService.sendMessage(userRequest.getChatId(),
                "Обирайте з меню нижче ⤵️");



    }

    @Override
    public boolean isGlobal() {
        return true;
    }
}
