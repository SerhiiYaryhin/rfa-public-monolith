package media.toloka.rfa.bot.tetegrambot;

// https://javarush.com/en/groups/posts/en.2959.create-a-telegram-bot-using-spring-boot


//import lombok.Value;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Properties;

//import static jdk.javadoc.internal.tool.Main.execute;


@Component
//@RequiredArgsConstructor
@Slf4j
public class TelegramBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    Logger logger = LoggerFactory.getLogger(TelegramBot.class);

    @Value("${media.toloka.rfa.telegram.token}")
    private String botToken;

    @Value("${media.toloka.rfa.telegram.name}")
    private String botName;

    private final TelegramClient telegramClient;

    private Properties prop;

    public TelegramBot() {
        telegramClient = new OkHttpTelegramClient(getBotToken());
    }

    // витягуємо параметри бота з файла конфігурації Телеграму.
    @Override
    public String getBotToken() {
        Properties prop = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream stream = loader.getResourceAsStream("telegram.properties");
        try {
            prop.load(stream);
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("ERROR: Не можу завантажити файл з параметрами телеграму.");
            return null;
        }
        String token = prop.getProperty("telegram.token");
        if (token != null) {
            return token;
        }
        return null;
//        return "1856110317:AAEem1mOzK96bRDWPO9amEctmUF8dtTXBWM";
//        return botToken;
    }
//    public String getBotToken() {
//    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        // We check if the update has a message and the message has text
        logger.info("=============== Start prepare");
        if (update.hasMessage()) {
            long chat_id = update.getMessage().getChatId();
            if (update.getMessage().hasText()) {
                logger.info("=============== Text");
                // Set variables
                String message_text = update.getMessage().getText();
//                long chat_id = update.getMessage().getChatId();
                SendMessage message = SendMessage // Create a message object
                        .builder()
                        .chatId(chat_id)
                        .text(message_text)
                        .build();
                try {
                    telegramClient.execute(message); // Sending our message object to user
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
            // get photo
            else if (update.getMessage().hasPhoto()) {
                logger.info("=============== Photo");
//            String message_text = update.getMessage().getCaption();
                List<PhotoSize> photos = update.getMessage().getPhoto();
                logger.info("============== List quantity: {}", photos.size());
                // Know file_id
                String f_id = photos.stream().max(Comparator.comparing(PhotoSize::getFileSize))
                        .map(PhotoSize::getFileId)
                        .orElse("");
                // Know photo width
                int f_width = photos.stream().max(Comparator.comparing(PhotoSize::getFileSize))
                        .map(PhotoSize::getWidth)
                        .orElse(0);
                // Know photo height
                int f_height = photos.stream().max(Comparator.comparing(PhotoSize::getFileSize))
                        .map(PhotoSize::getHeight)
                        .orElse(0);
                int f_size = photos.stream().max(Comparator.comparing(PhotoSize::getFileSize))
                        .map(PhotoSize::getFileSize)
                        .orElse(0);
                // Завантажуємо фото
                try {
                    Document document = new Document();
                    document.setMimeType(update.getMessage().getDocument().getMimeType());
                    document.setFileName(update.getMessage().getDocument().getFileName());
                    document.setFileSize(update.getMessage().getDocument().getFileSize());
                    document.setFileId(f_id);
                    downloadFile(document, "/home/ysv/Clients/bot/Photo_"+update.getMessage().getDocument().getFileId()+"_"+update.getMessage().getDocument().getFileName());
                    logger.info("Записали документ.");
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.error("Помилка при запису документу.");
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

                // Set photo caption "Message: "+message_text+
                String caption = "file_id: " + f_id
                        + "\nwidth: " + Integer.toString(f_width)
                        + "\nheight: " + Integer.toString(f_height);
                SendPhoto msg = SendPhoto
                        .builder()
                        .chatId(chat_id)
                        .photo(new InputFile(f_id))
                        .caption(caption)
                        .build();
                try {
                    telegramClient.execute(msg); // Sending our message object to user
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
//            else if (update.getMessage().hasAudio())
//            {
//                logger.info("=============== Audio");
//                long chat_id = update.getMessage().getChatId();
//                Audio audio = update.getMessage().getAudio();
//                String a_id = audio.getFileId();
//                String a_fn = audio.getFileName();
//                String a_mime = audio.getMimeType();
//                Long a_size = audio.getFileSize();
//                Integer a_duration = audio.getDuration();
//            }
            else if (update.getMessage().hasDocument())
            {
                logger.info("=============== Document");
                String doc_id = update.getMessage().getDocument().getFileId();
                String doc_name = update.getMessage().getDocument().getFileName();
                String doc_mine = update.getMessage().getDocument().getMimeType();
                Long doc_size = update.getMessage().getDocument().getFileSize();
                String getID = String.valueOf(update.getMessage().getFrom().getId());

                Document document = new Document();
                document.setMimeType(doc_mine);
                document.setFileName(doc_name);
                document.setFileSize(doc_size);
                document.setFileId(doc_id);
                try {
                    downloadFile(document, "/home/ysv/Clients/bot/"+getID+"_"+doc_name);
                    logger.info("Записали документ.");
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.error("Помилка при запису документу.");
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

                GetFile getFile = new GetFile(document.getFileId());
                try {
                    org.telegram.telegrambots.meta.api.objects.File file = telegramClient.execute(getFile);
                    logger.info("Відправили документ.");
//                    downloadFile(document, "/home/ysv/Clients/bot/"+getID+"_"+doc_name);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
                String caption = "doc_id: " + doc_id + "\nname: " + doc_name + "\nmime: " + doc_mine ;
//                SendPhoto msg = SendDocument
//                        .builder()
//                        .chatId(chat_id)
//                        .document(new InputFile(doc_id))
//                        .caption(caption)
//                        .build();
//                try {
//                    telegramClient.execute(msg); // Sending our message object to user
//                } catch (TelegramApiException e) {
//                    e.printStackTrace();
//                }

            }
//            else if (update.getMessage().hasVideo())
//            {
//                  logger.info("=============== Video");
//            }
        }
    }

    @AfterBotRegistration
    public void afterRegistration(BotSession botSession) {
        System.out.println("Registered bot running state is: " + botSession.isRunning());
    }

    public void downloadFile(Document document,  String localFilePath) throws IOException, TelegramApiException {
        File file = getFilePath(document);

        java.io.File localFile = new java.io.File(localFilePath);
        InputStream is = new URL(file.getFileUrl(getBotToken())).openStream();
//        InputStream is = new URL(file.get  getFileUrl(questionAnsweringBot.getBotToken())).openStream();
        FileUtils.copyInputStreamToFile(is, localFile);
    }

    public File getFilePath(Document document) throws TelegramApiException {
        GetFile getFile = new GetFile(document.getFileId());
//        getFile.setFileId(document.getFileId());
        File file = telegramClient.execute(getFile);
        return file;
    }
}
