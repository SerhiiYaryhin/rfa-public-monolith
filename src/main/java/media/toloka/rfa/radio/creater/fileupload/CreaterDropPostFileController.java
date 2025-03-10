package media.toloka.rfa.radio.creater.fileupload;

import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.document.service.DocumentService;
import media.toloka.rfa.radio.dropfile.service.FilesService;
import media.toloka.rfa.radio.history.service.HistoryService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.store.Service.StoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static media.toloka.rfa.radio.store.model.EStoreFileType.*;

@Profile("Front")
@Slf4j
@RestController
//@RequestMapping("/uploadfile")
public class CreaterDropPostFileController {

    @Value("${media.toloka.rfa.upload_directory}")
    private String PATHuploadDirectory;

    @Autowired
    private ClientService clientService;
    @Autowired
    private DocumentService documentService;

    @Autowired
    private CreaterService createrService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private FilesService filesService;

    @Autowired
    private StoreService storeService;

    final Logger logger = LoggerFactory.getLogger(CreaterDropPostFileController.class);

    @PostMapping(path = "/creater/trackupload" ) // , produces = MediaType.APPLICATION_JSON_VALUE
    public void uploadTrack(@RequestParam("file") MultipartFile file) {

//        log.info("uploaded file " + file.getOriginalFilename());
        if (file.isEmpty()) {
//                throw new ExecutionControl.UserException("Empty file");
            logger.info("Завантаження файлу: Файл порожній");
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (clientService.ClientCanDownloadFile(cd) == false) {
            // клієнт з якоїсь причини не має права завантажувати файли
            logger.warn("Клієнт {} не має права завантажувати файли.", cd.getUuid());
            return;
        }
        try {
            String storeUUID = storeService.PutFileToStore(file.getInputStream(),file.getOriginalFilename(),cd,STORE_TRACK);
            createrService.SaveTrackUploadInfo(storeUUID, cd);
        } catch (IOException e) {
            logger.info("Завантаження файлу: Проблема збереження");
            e.printStackTrace();
        }
        log.info("uploaded file " + file.getOriginalFilename());

    }

    @PostMapping(path = "/creater/albumcoverupload" ) // , produces = MediaType.APPLICATION_JSON_VALUE
    public void uploadAlbumCover(@RequestParam("file") MultipartFile file) {

        log.info("Завантажуємо обкладинку " + file.getOriginalFilename());
        if (file.isEmpty()) {
//                throw new ExecutionControl.UserException("Empty file");
            logger.info("Завантаження файлу: Файл порожній");
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (clientService.ClientCanDownloadFile(cd) == false) {
            // клієнт з якоїсь причини не має права завантажувати файли
            logger.warn("Клієнт {} не має права завантажувати файли.", cd.getUuid());
            return;
        }

        try {
            // тестуємо завантаження через сервіси сховища.
            String storeUUID = storeService.PutFileToStore(file.getInputStream(),file.getOriginalFilename(),cd,STORE_ALBUMCOVER);
            createrService.SaveAlbumCoverUploadInfo(storeUUID,cd);
            // todo додати storeUUID до альбому
        } catch (IOException e) {
            logger.info("Завантаження файлу: Проблема збереження");
            e.printStackTrace();
        }
        log.info("uploaded file " + file.getOriginalFilename());

    }

    @PostMapping(path = "/creater/picprofileupload" ) // , produces = MediaType.APPLICATION_JSON_VALUE
    public void uploadPicProfile(@RequestParam("file") MultipartFile file) {

        log.info("Завантажуємо фото профайлу " + file.getOriginalFilename());
        if (file.isEmpty()) {
//                throw new ExecutionControl.UserException("Empty file");
            logger.info("Завантаження файлу: Файл порожній");
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (clientService.ClientCanDownloadFile(cd) == false) {
            // клієнт з якоїсь причини не має права завантажувати файли
            logger.warn("Клієнт {} не має права завантажувати файли.", cd.getUuid());
            return;
        }

        try {
            // тестуємо завантаження через сервіси сховища.
            String storeUUID = storeService.PutFileToStore(file.getInputStream(),file.getOriginalFilename(),cd,STORE_PHOTO);
            createrService.SaveAlbumCoverUploadInfo(storeUUID,cd);
            // !!! todo додати storeUUID до профайлу
        } catch (IOException e) {
            logger.info("Завантаження файлу: Проблема збереження");
            e.printStackTrace();
        }
        log.info("uploaded file " + file.getOriginalFilename());

    }


    @PostMapping(path = "/creater/storefileupload" ) // , produces = MediaType.APPLICATION_JSON_VALUE
    public void uploadStore(@RequestParam("file") MultipartFile file) {

        log.info("Завантажуємо файл у сховище " + file.getOriginalFilename());
        if (file.isEmpty()) {
//                throw new ExecutionControl.UserException("Empty file");
            logger.info("Завантаження файлу: Файл порожній");
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (clientService.ClientCanDownloadFile(cd) == false) {
            // клієнт з якоїсь причини не має права завантажувати файли
            logger.warn("Клієнт {} не має права завантажувати файли.", cd.getUuid());
            return;
        }

        try {
            // тестуємо завантаження через сервіси сховища.
            String storeUUID = storeService.PutFileToStore(file.getInputStream(),file.getOriginalFilename(),cd,STORE_FILE);


        } catch (IOException e) {
            logger.info("Завантаження файлу: Проблема збереження");
            e.printStackTrace();
        }
        log.info("uploaded file " + file.getOriginalFilename());

    }

    // Старі версії.


}

