package media.toloka.rfa.tetegrambot.handler.impl.link2rfa;


import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.login.service.TokenService;
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

@Slf4j
@Component
public class LinkTelegramGetUUIDTokenEnteredHandler extends UserRequestHandler {

    @Autowired
    private ClientService clientService;
    @Autowired
    private TokenService serviceToken;

    private final TelegramService telegramService;
    private final KeyboardHelper keyboardHelper;
    private final UserSessionService userSessionService;

    public LinkTelegramGetUUIDTokenEnteredHandler(TelegramService telegramService, KeyboardHelper keyboardHelper, UserSessionService userSessionService) {
        this.telegramService = telegramService;
        this.keyboardHelper = keyboardHelper;
        this.userSessionService = userSessionService;
    }

    @Override
    public boolean isApplicable(UserRequest userRequest) { //return isTextMessage(userRequest.getUpdate(), BTN_TELEGRAM_LINK);
        return isTextMessage(userRequest.getUpdate())
                && ConversationState.WAITING_CD_FOR_TELEGRAM_LINK_UUID.equals(userRequest.getUserSession().getState());
    }

    @Override
    public void handle(UserRequest userRequest) {
        ReplyKeyboardMarkup replyKeyboardMarkup = keyboardHelper.buildMenuWithCancel();
        telegramService.sendMessage(userRequest.getChatId(),
                "✍️Тепер ми спробуємо привʼязати Ваш Телеграм та обліковий запис на порталі️. "
                        +"Будь ласка, скопіюйте та відправте ідентифікатор, який Ви можете отримати у Вашому профайлі.",
                replyKeyboardMarkup);


        UserSession userSession = userRequest.getUserSession();

        String RFA_Telegram_UUID = userRequest.getUpdate().getMessage().getText();
        log.info("RFA_Telegram_UUID: {}",RFA_Telegram_UUID);
//        Users rfa_user = clientService.setTelegramLink(RFA_Telegram_UUID, userRequest);
//        Clientdetail cd = null;
        // прописуємо ідентифікатор клієнта в базі.
        if (clientService.setTelegramLink(RFA_Telegram_UUID, userRequest)) {
            log.info("прописуємо ідентифікатор клієнта в базі Користувачів.");
            // повідомлення про успіх
            replyKeyboardMarkup = keyboardHelper.buildMainMenu();
            telegramService.sendMessage(userRequest.getChatId(),
                    "Вітаємо! Тепер Ваш аккаунт в телеграмі привʼязаний до порталу \"Радіо для всіх!\"!");
            telegramService.sendMessage(userRequest.getChatId(),
                    "✍️Тепер Ви можете завантажувати треки на портал \"Радіо для всіх!\"! ",
                    replyKeyboardMarkup);
        }
        else {
            // щось пішло не так - не знайшли токен в базі
            log.info("Користувач {} Не знайшли токен {} в базі.",  userRequest.getUpdate().getMessage().getFrom().getId(), RFA_Telegram_UUID);
            telegramService.sendMessage(userRequest.getChatId(),
                    "Щось пішло не так :( \n Спробуйте ще раз або зверніться до служби підтримки.");
        }

        userSession.setState(ConversationState.CONVERSATION_STARTED);
        userSessionService.saveSession(userSession.getChatId(), userSession);

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

//        UserSession session = userRequest.getUserSession();
////        session.setCity(city);
//        session.setState(ConversationState.WAITING_CD_FOR_TELEGRAM_LINK_UUID);
//        userSessionService.saveSession(userRequest.getChatId(), session);
    }

    @Override
    public boolean isGlobal() {
        return false;
    }

}
