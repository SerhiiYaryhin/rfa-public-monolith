package media.toloka.rfa.tetegrambot.service;

import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.tetegrambot.TelegramBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * This service allows to communicate with Telegram API
 */
@Slf4j
@Component
public class TelegramService {

    @Autowired
    private  TelegramBot telegramBot;


//    private final TelegramClient botSender;




//    public TelegramService(TelegramBot botSender) {
//        this.botSender = telegramBot.getTelegramClient();
//    }

    public void sendMessage(Long chatId, String text) {
        sendMessage(chatId, text, null);
    }

    public void sendMessage(Long chatId, String text, ReplyKeyboard replyKeyboard) {
        SendMessage sendMessage = SendMessage
                .builder()
                .text(text)
                .chatId(chatId.toString())
                //Other possible parse modes: MARKDOWNV2, MARKDOWN, which allows to make text bold, and all other things
                .parseMode(ParseMode.HTML)
                .replyMarkup(replyKeyboard)
                .build();
        execute(sendMessage);
    }

    private void execute(BotApiMethod botApiMethod) {
        try {
            telegramBot.getTelegramClient().execute(botApiMethod);
        } catch (Exception e) {
            log.error("Exception: ", e);
        }
    }
}
