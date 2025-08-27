package media.toloka.rfa.banner.fileupload;

import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.banner.model.Banner;
import media.toloka.rfa.banner.model.enumerate.EBannerType;
import media.toloka.rfa.banner.service.BannerService;
import media.toloka.rfa.podcast.model.PodcastChannel;
import media.toloka.rfa.podcast.model.PodcastItem;
import media.toloka.rfa.podcast.service.PodcastService;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.dropfile.service.FilesService;
import media.toloka.rfa.radio.history.service.HistoryService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.EStoreFileType;
import media.toloka.rfa.radio.store.model.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static media.toloka.rfa.radio.store.model.EStoreFileType.*;


@Slf4j
@RestController
@RequestMapping("/banners")
public class BannerDropPostFileController {

    @Value("${media.toloka.rfa.upload_directory}")
    private String PATHuploadDirectory;

    @Autowired
    private BannerService bannerService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private FilesService filesService;

    @Autowired
    private StoreService storeService;

    final Logger logger = LoggerFactory.getLogger(BannerDropPostFileController.class);

    @PostMapping(path = "/mediaupload/{buuid}" )
    public void BannerMediaUpload(
            @PathVariable String buuid,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            logger.info("Завантаження баннера: Файл порожній");
            return;
        }
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (clientService.ClientCanDownloadFile(cd) == false) {
            // клієнт з якоїсь причини не має права завантажувати файли
            logger.warn("Клієнт {} не має права завантажувати файли.", cd.getUuid());
            return;
        }
        Banner banner = bannerService.BannerGetByUUID(buuid);
        log.info("Current banner {} {}",buuid, banner.getUuid());
        try {
            String storeUUID = storeService.PutFileToStore(
                    file.getInputStream(),
                    file.getOriginalFilename(),cd,STORE_DUMMY);
            // завантажили файл
            // визначаємо тип баннера
            banner.setUuidmedia(storeUUID);
            Store store = storeService. GetStoreByUUID(storeUUID);
            String mediatype = store.getContentMimeType().substring(0, store.getContentMimeType().indexOf('/')).toUpperCase();
            try {
                store.setStorefiletype  ( EStoreFileType.valueOf("STORE_BANNER" + mediatype));
                storeService.SaveStore(store);
            } catch (IllegalArgumentException eae) {
                logger.info("Промахнулися з типом медіа у сховищі");
            }
            try {
                banner.setBannertype(EBannerType.valueOf(mediatype));
                banner.setStore(store);
                bannerService.BannerSave(banner);
            } catch (IllegalArgumentException eae) {
                logger.info("Промахнулися з типом банера");
            }

        } catch (IOException e) {
            logger.info("Завантаження файлу: Проблема збереження");
        }
        log.info("uploaded file {} \n Тип банера: {}",file.getOriginalFilename(),banner.getBannertype().toString());
    }


}

