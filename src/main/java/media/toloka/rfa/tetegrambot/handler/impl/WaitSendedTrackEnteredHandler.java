package media.toloka.rfa.tetegrambot.handler.impl;


import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.tetegrambot.enums.ConversationState;
import media.toloka.rfa.tetegrambot.handler.UserRequestHandler;
import media.toloka.rfa.tetegrambot.helper.KeyboardHelper;
import media.toloka.rfa.tetegrambot.model.UserRequest;
import media.toloka.rfa.tetegrambot.model.UserSession;
import media.toloka.rfa.tetegrambot.service.TelegramService;
import media.toloka.rfa.tetegrambot.service.UserSessionService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Audio;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import static media.toloka.rfa.tetegrambot.constant.Constants.BTN_TELEGRAM_LINK;
import static media.toloka.rfa.tetegrambot.enums.ConversationState.WAITING_SEND_TRACK;

@Slf4j
@Component
public class WaitSendedTrackEnteredHandler extends UserRequestHandler {

    private final TelegramService telegramService;
    private final KeyboardHelper keyboardHelper;
    private final UserSessionService userSessionService;

    public WaitSendedTrackEnteredHandler(TelegramService telegramService, KeyboardHelper keyboardHelper, UserSessionService userSessionService) {
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
                Audio audio = userRequest.getUpdate().getMessage().getAudio();
                String a_id = audio.getFileId();
                String a_fn = audio.getFileName();
                String a_mime = audio.getMimeType();
                Long a_size = audio.getFileSize();
                Integer a_duration = audio.getDuration();
                log.info("FileID: {}, FileName: {} , Mime: {}, Size: {}, Duration: {} ",a_id, a_fn,a_mime,a_size,a_duration);

        // Таки вдалося завантажити
        telegramService.sendMessage(userRequest.getChatId(),
                "Ваш трек отримано."
                +"\nFileID: "+a_id
                +"\nFileName: "+ a_fn
                +"\nMime: "+a_mime
                +"\nSize: "+a_size.toString()
                +"\nDuration: "+a_duration.toString(),
                replyKeyboardMarkup);

        ReplyKeyboard replyKeyboard = keyboardHelper.buildMainMenu();
        telegramService.sendMessage(userRequest.getChatId(),
                "\uD83D\uDC4BПривіт! За допомогою цього чат-бота ви зможете надіслати свої треки на портал \"Радіо для всіх!\"",
                replyKeyboard);
//        String RFA_Telegram_UUID = userRequest.getUpdate().getMessage().getText();
////        log.info("RFA_Telegram_UUID: ",RFA_Telegram_UUID);
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
