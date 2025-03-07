package media.toloka.rfa.tetegrambot.handler.impl.global;


import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.tetegrambot.handler.UserRequestHandler;
import media.toloka.rfa.tetegrambot.helper.KeyboardHelper;
import media.toloka.rfa.tetegrambot.model.UserRequest;
import media.toloka.rfa.tetegrambot.service.TelegramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
//import org.vladyka.handler.UserRequestHandler;
//import org.vladyka.helper.KeyboardHelper;
//import org.vladyka.model.UserRequest;
//import org.vladyka.service.TelegramService;

@Profile("Telegram")
@Component
public class StartCommandHandler extends UserRequestHandler {

    @Autowired
    private ClientService clientService;

    private static String command = "/start";

    private final TelegramService telegramService;
    private final KeyboardHelper keyboardHelper;

    public StartCommandHandler(TelegramService telegramService, KeyboardHelper keyboardHelper) {
        this.telegramService = telegramService;
        this.keyboardHelper = keyboardHelper;
    }

    @Override
    public boolean isApplicable(UserRequest userRequest) {
        return isCommand(userRequest.getUpdate(), command);
    }

    @Override
    public void handle(UserRequest request) {

//        Clientdetail cd =  clientService.GetUserFromTelegram(request.getUpdate().getMessage().getFrom().getId().toString());
        if (clientService.GetUserFromTelegram(request.getUpdate().getMessage().getFrom().getId().toString()) != null) {
            ReplyKeyboard replyKeyboard = keyboardHelper.buildMainMenu();
            telegramService.sendMessage(request.getChatId(),
                    "\uD83D\uDC4BПривіт! За допомогою цього чат-бота ви зможете взаємодіяти з порталом \"Радіо для всіх!\""
                            +" надіслати свої треки, додати опис треку, надіслати повідомлення в прямий ефір Студії Толока і багато всього іншого.",
                    replyKeyboard);
        }
        else {
            ReplyKeyboard replyKeyboard = keyboardHelper.buildRegisterMenu();
            telegramService.sendMessage(request.getChatId(),
                    "\uD83D\uDC4BПривіт! За допомогою цього чат-бота ви зможете взаємодіяти з порталом \"Радіо для всіх!\""
                            +" надіслати свої треки, додати опис треку, надіслати повідомлення в прямий ефір Студії Толока і багато всього іншого.",
                    replyKeyboard);
            telegramService.sendMessage(request.getChatId(),
                    "Для цього Ви повинні бути зареєстровані на порталі https://rfa.toloka.media/ "
                            +"та привʼязати свій Телеграм до облікового запису на порталі.");

        }

        telegramService.sendMessage(request.getChatId(),
                "Обирайте з меню нижче ⤵️");
    }

    @Override
    public boolean isGlobal() {
        return true;
    }
}
