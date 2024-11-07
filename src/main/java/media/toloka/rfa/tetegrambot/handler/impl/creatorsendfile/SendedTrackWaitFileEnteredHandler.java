package media.toloka.rfa.tetegrambot.handler.impl.creatorsendfile;


import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.dropfile.service.FilesService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.tetegrambot.enums.ConversationState;
import media.toloka.rfa.tetegrambot.handler.UserRequestHandler;
import media.toloka.rfa.tetegrambot.helper.KeyboardHelper;
import media.toloka.rfa.tetegrambot.model.UserRequest;
import media.toloka.rfa.tetegrambot.model.UserSession;
import media.toloka.rfa.tetegrambot.service.TelegramFileService;
import media.toloka.rfa.tetegrambot.service.TelegramService;
import media.toloka.rfa.tetegrambot.service.UserSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Audio;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static media.toloka.rfa.radio.store.model.EStoreFileType.STORE_TRACK;

@Slf4j
@Component
public class SendedTrackWaitFileEnteredHandler extends UserRequestHandler {

    @Value("${media.toloka.rfa.upload_directory}")
    private String PATHuploadDirectory;

    @Autowired
    private TelegramFileService telegramFileService;

    @Autowired
    private ClientService clientService;



    private final TelegramService telegramService;
    private final KeyboardHelper keyboardHelper;
    private final UserSessionService userSessionService;

    public SendedTrackWaitFileEnteredHandler(TelegramService telegramService, KeyboardHelper keyboardHelper, UserSessionService userSessionService) {
        this.telegramService = telegramService;
        this.keyboardHelper = keyboardHelper;
        this.userSessionService = userSessionService;
    }

    @Override
    public boolean isApplicable(UserRequest userRequest) {
//        return isTextMessage(userRequest.getUpdate(), WAITING_SEND_TRACK);
//        return (isAudioMessage(userRequest.getUpdate()) || isDocumentMessage(userRequest.getUpdate()))
        return isAudioMessage(userRequest.getUpdate())
                && ConversationState.WAITING_SEND_TRACK.equals(userRequest.getUserSession().getState());
    }

    @Override
    public void handle(UserRequest userRequest) {
        ReplyKeyboardMarkup replyKeyboardMarkup = keyboardHelper.buildMenuWithCancel();

        // Працюємо з завантаженням треку
                log.info("=============== Audio");
                long chat_id = userRequest.getUpdate().getMessage().getChatId();
                String caption = userRequest.getUpdate().getMessage().getCaption();
                Audio audio = userRequest.getUpdate().getMessage().getAudio();
                String a_id = audio.getFileId();
                String a_fn = audio.getFileName();
                String a_mime = audio.getMimeType();
                Long a_size = audio.getFileSize();
                Integer a_duration = audio.getDuration();
                log.info("\nCaption: {}\nFileID: {}\nFileName: {}\nMime: {}\nSize: {}\nDuration: {} ",caption ,a_id, a_fn,a_mime,a_size,a_duration);

        // Таки вдалося завантажити
        telegramService.sendMessage(userRequest.getChatId(),
                "Ваш трек отримано."
                +"\nCaption: "+caption
                +"\nFileID: "+a_id
                +"\nFileName: "+ a_fn
                +"\nMime: "+a_mime
                +"\nSize: "+a_size.toString()
                +"\nDuration: "+a_duration.toString()+"\n",
                replyKeyboardMarkup);

        // Завантажуємо файл mp3
        // отримуємо cd користувача в телеграмі
        Clientdetail cd = clientService.GetUserFromTelegram(userRequest.getUpdate().getMessage().getFrom().getId().toString());
        String filename = a_fn;

//        String storePath = "/home/ysv/Clients/bot/"+a_id+"_"+a_fn;

        try {
            telegramFileService.downloadFile(a_id,a_fn,cd);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("SendedTrackWaitFileEnteredHandler. Помилка при запису документу.");
        } catch (TelegramApiException e) {
            e.printStackTrace();
            log.error("SendedTrackWaitFileEnteredHandler. Помилка інтерфейсу Телеграма.");
        }


        ReplyKeyboard replyKeyboard = keyboardHelper.buildMainMenu();
        telegramService.sendMessage(userRequest.getChatId(),
                "\uD83D\uDC4BПривіт! За допомогою цього чат-бота ви зможете взаємодіяти з порталом \"Радіо для всіх!\""
                        +" надіслати свої треки, додати опис треку, надіслати повідомлення в прямий ефір Студії Толока і багато всього іншого.",
                replyKeyboard);

        UserSession session = userRequest.getUserSession();
////        session.setCity(city);
        session.setState(ConversationState.CONVERSATION_STARTED);
//        userSessionService.saveSession(userRequest.getChatId(), session);
    }

    @Override
    public boolean isGlobal() {
        return false;
    }

}
