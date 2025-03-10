package media.toloka.rfa.radio.dropfile;

import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.document.service.DocumentService;
import media.toloka.rfa.radio.dropfile.service.FilesService;
import media.toloka.rfa.radio.history.service.HistoryService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.store.Service.StoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static media.toloka.rfa.radio.store.model.EStoreFileType.STORE_DOCUMENT;


@Slf4j
@Profile("Front")
@RestController
public class DropPostFileController {
// Завантажуємо документи клієнта
    @Autowired
    private ClientService clientService;
    @Autowired
    private DocumentService documentService;
    @Autowired
    private HistoryService historyService;

    @Autowired
    private FilesService filesService;

    @Autowired
    private StoreService storeService;

    final Logger logger = LoggerFactory.getLogger(DropPostFileController.class);

    @PostMapping(path = "/uploadfile" ) // , produces = MediaType.APPLICATION_JSON_VALUE
    public void upload(@RequestParam("file") MultipartFile file) {

        log.info("uploaded file " + file.getOriginalFilename());
        if (file.isEmpty()) {
//                throw new ExecutionControl.UserException("Empty file");
            logger.info("Завантаження файлу: Файл порожній");
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        // todo обробка права завантажувати файли
        if (clientService.ClientCanDownloadFile(cd) == false) {
            // клієнт з якоїсь причини не має права завантажувати файли
            logger.warn("Клієнт {} не має права завантажувати файли.",cd.getUuid());
            return;
        }

        try {
            // тестуємо завантаження через сервіси сховища.
            String storeUUID = storeService.PutFileToStore(file.getInputStream(),file.getOriginalFilename(),cd,STORE_DOCUMENT);
            documentService.SaveDocumentUploadInfo(storeUUID,  cd, storeService.GetStoreByUUID(storeUUID));
        } catch (IOException e) {
            logger.info("Завантаження файлу: Проблема збереження");
            e.printStackTrace();
        }
        log.info("uploaded file " + file.getOriginalFilename());
    }

}

