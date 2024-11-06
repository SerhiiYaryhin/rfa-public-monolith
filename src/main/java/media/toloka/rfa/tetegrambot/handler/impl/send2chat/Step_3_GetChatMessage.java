package media.toloka.rfa.tetegrambot.handler.impl.send2chat;
// Надсилаємо повідомлення в чат прямого ефіру
// перелік прямих ефірів беремо з чату
import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.media.messanger.service.MessangerService;
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

@Slf4j
@Component
public class Step_3_GetChatMessage extends UserRequestHandler {

    @Autowired
    private MessangerService messangerService;
    @Autowired
    private ClientService clientService;

    private final TelegramService telegramService;
    private final KeyboardHelper keyboardHelper;
    private final UserSessionService userSessionService;

    public Step_3_GetChatMessage(TelegramService telegramService, KeyboardHelper keyboardHelper, UserSessionService userSessionService) {
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
                && ConversationState.WAITING_CHAT_MESSAGE.equals(userRequest.getUserSession().getState());
    }

    @Override
    public void handle(UserRequest userRequest) {
        ReplyKeyboardMarkup replyKeyboardMarkup = keyboardHelper.buildMenuWithCancel();
        // Отримали введене значення


        // Перевіряємо, чи чат в онлайні
        if (messangerService.GetChatRoomById(userRequest.getUserSession().getRFA_Chat_Choose().getId()).getRoomOnlineStatus()) {
            telegramService.sendMessage(userRequest.getChatId(),
                    "Ви надсилаєте повідомлення до чату: \n"+userRequest.getUserSession().getRFA_Chat_Choose().getRoomname());

            telegramService.sendMessage(userRequest.getChatId(),
                    "Введіть повідомлення та надішліть\nДля завершення натисніть \"Скасувати\"",
                    replyKeyboardMarkup);
            // формуємо перелік чатів і нумеруємо їх
            log.info(userRequest.getUpdate().getMessage().getText());
        } else {// Чат вийшов з онлайну
            // Встановлюємо стан для наступного кроку
            log.info("Прямий ефір завершився. Виходимо з чату.");
            telegramService.sendMessage(userRequest.getChatId(),
                    "Прямий ефір завершився. Виходимо з чату: \n"+userRequest.getUserSession().getRFA_Chat_Choose().getRoomname());
            UserSession session = userRequest.getUserSession();
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

        // візначаємо наступний крок
//        UserSession session = userRequest.getUserSession();
//        session.setState(ConversationState.WAITING_CHAT_MESSAGE);
//        userSessionService.saveSession(userRequest.getChatId(), session);
    }
}
