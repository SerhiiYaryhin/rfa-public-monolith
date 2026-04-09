package media.toloka.rfa.tetegrambot;

// https://javarush.com/en/groups/posts/en.2959.create-a-telegram-bot-using-spring-boot


//import lombok.Value;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.tetegrambot.model.UserRequest;
import media.toloka.rfa.tetegrambot.model.UserSession;
import media.toloka.rfa.tetegrambot.service.UserSessionService;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;


//import static jdk.javadoc.internal.tool.Main.execute;

@Profile("Telegram")
@Component
//@RequiredArgsConstructor
@Slf4j
public class TelegramBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

//    @Value("${media.toloka.rfa.telegram.token}")

    @Autowired
    private Environment env;

    @Value("${TELEGRAMBOTKEY}")
    private String botToken;

    @Value("${TELEGRAMBOTNAME}")
    private String botName;

    private final TelegramClient telegramClient;
//    @Autowired
//    public final List<UserRequestHandler> handlers = new ArrayList<>();

//    @Autowired
    private Dispatcher dispatcher;// = new Dispatcher(handlers);

//    @Autowired
    private  UserSessionService userSessionService;

    private Properties prop;

    public TelegramBot(/* Dispatcher dispatcher, */ UserSessionService userSessionService) {
        telegramClient = new OkHttpTelegramClient(getBotToken());
//        this.dispatcher1 = dispatcher;
        this.userSessionService = userSessionService;
    }

    public TelegramClient getTelegramClient() {
        return telegramClient;
    }

    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    // витягуємо параметри бота для конфігурації Телеграму.
    @Override
    public String getBotToken() {
//         varValue = botName;
//        log.info("Current botName TELEGRAM BOT: {}",botName);
        String varValue = System.getenv("TELEGRAMBOTNAME");
        log.info("Current TELEGRAM BOT: {}",varValue);
//        varValue = botToken;
//        log.info("Current botToken TELEGRAM BOT: {}",varValue);
//        if (varValue != null) return varValue;
        varValue = System.getenv("TELEGRAMBOTKEY");
//        log.info("Current env TELEGRAM BOT: {}",varValue);
        if (varValue != null) return varValue;
        return null;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        // We check if the update has a message and the message has text
        log.info("=============== Start prepare message.");
//        if(update.hasMessage() && update.getMessage().hasText()) {
        if(update.hasMessage() ) {
            String textFromUser = update.getMessage().getText();

            Long userId = update.getMessage().getFrom().getId();
            String userFirstName = update.getMessage().getFrom().getFirstName();

            log.info("[{}, {}] : {}", userId, userFirstName, textFromUser);

            Long chatId = update.getMessage().getChatId();
            UserSession session = userSessionService.getSession(chatId);

            UserRequest userRequest = UserRequest
                    .builder()
                    .update(update)
                    .userSession(session)
                    .chatId(chatId)
                    .build();

            boolean dispatched = dispatcher.dispatch(userRequest);

            if (!dispatched) {
                log.warn("Unexpected dispatch update from user internal");
            }
            return;
        }
    }

    @AfterBotRegistration
    public void afterRegistration(BotSession botSession) {
        System.out.println("Registered bot running state is: " + botSession.isRunning());
    }
}
