package media.toloka.rfa.tetegrambot.service;

import media.toloka.rfa.tetegrambot.TelegramBot;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

@Component
public class TelegramFileService {

    @Autowired
    private TelegramBot telegramBot;

//    public void downloadFile(Document document, String localFilePath) throws IOException, TelegramApiException {
    public void downloadFile(String file_id, String localFilePath) throws IOException, TelegramApiException {
        File file = getFilePath(file_id);

        java.io.File localFile = new java.io.File(localFilePath);
        InputStream is = new URL(file.getFileUrl(telegramBot.getBotToken())).openStream();
//        InputStream is = new URL(file.get  getFileUrl(questionAnsweringBot.getBotToken())).openStream();
        FileUtils.copyInputStreamToFile(is, localFile);
    }

    public File getFilePath(String file_id) throws TelegramApiException {
        GetFile getFile = new GetFile(file_id);
        File file = telegramBot.getTelegramClient().execute(getFile);
        return file;
    }
}
