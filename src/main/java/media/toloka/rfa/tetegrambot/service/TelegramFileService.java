package media.toloka.rfa.tetegrambot.service;

import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.dropfile.service.FilesService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.store.Service.StoreService;
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

import static media.toloka.rfa.radio.store.model.EStoreFileType.STORE_TRACK;

@Slf4j
@Component
public class TelegramFileService {
    @Autowired
    private FilesService filesService;

    @Autowired
    private CreaterService createrService;

    @Autowired
    private StoreService storeService;

    @Autowired
    private TelegramBot telegramBot;

//    public void downloadFile(Document document, String localFilePath) throws IOException, TelegramApiException {
    public void downloadFile(String file_id, String filename, Clientdetail cd) throws IOException, TelegramApiException {
        File file = getFilePath(file_id);

//        java.io.File localFile = new java.io.File(localFilePath);
        InputStream is = new URL(file.getFileUrl(telegramBot.getBotToken())).openStream();

        try {
            String storeUUID = storeService.PutFileToStore(is,filename,cd,STORE_TRACK);
            createrService.SaveTrackUploadInfo(storeUUID, cd);
        } catch (IOException e) {
            log.info("Завантаження файлу: Проблема збереження");
            e.printStackTrace();
        }

//        InputStream is = new URL(file.get  getFileUrl(questionAnsweringBot.getBotToken())).openStream();
//        FileUtils.copyInputStreamToFile(is, localFile);
    }

    public File getFilePath(String file_id) throws TelegramApiException {
        GetFile getFile = new GetFile(file_id);
        File file = telegramBot.getTelegramClient().execute(getFile);
        return file;
    }
}
