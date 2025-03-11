package media.toloka.rfa.radio.store.implementation;

import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.store.model.EStoreFileType;
//import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.document.service.DocumentService;
import media.toloka.rfa.radio.dropfile.service.FilesService;
import media.toloka.rfa.radio.store.Interface.StoreInterface;
import media.toloka.rfa.radio.store.Reposirore.StoreRepositorePagination;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.radio.history.service.HistoryService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.security.model.Users;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static media.toloka.rfa.radio.model.enumerate.EHistoryType.History_DocumentCreate;

public class StoreFileImplementation implements StoreInterface {

    //@Value("${media.toloka.rfa.upload_directory}")
    //private String PATHuploadDirectory;

    @Autowired
    private StoreRepositorePagination storerepositore;

    @Autowired
    private FilesService filesService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private StoreRepositorePagination storeRepositore;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ClientService clientService;

    final Logger logger = LoggerFactory.getLogger(StoreFileImplementation.class);

    @Override
    public InputStream GetFileFromStore(String uuid) {
        Store store = storerepositore.getByUuid(uuid);
        File file = new File(store.getFilepatch());
        if (file.exists()) {
            try {
                InputStream is = new FileInputStream(file);
                return is;
            } catch (Exception ex) {
                logger.warn("======= Exception -> StoreFileImplementation GetFileFromStore FileNotFoundException");
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * з потоку записуємо файл у сховище. Поки не обробляю помилки запису. Доробити
     * @param inputStream вхідний потік з файлом, який зберігається до сховища
     * @param filename імʼя файлу для збереження
     * @param cd поточний авторізований користувач
     * @param storeFileType функціональне призначення файлу, що зберігається
     * @return String UUID елемента у сховищі
     */
    @Override
    public String PutFileToStore(InputStream inputStream, String filename, Clientdetail cd, EStoreFileType storeFileType) {
        Path destination = Paths.get(filesService.GetBaseClientDirectory(cd) + "/" + filesService.GetUploadDirectory()).resolve(filename).normalize().toAbsolutePath();
        Boolean fileExist = Files.exists(destination);

        try {
            Files.createDirectories(destination.getParent());
            Files.copy(inputStream, destination, REPLACE_EXISTING);
            inputStream.close();

        } catch (IOException e) {
            //
            logger.warn("RFAIOExeption StoreFileImplementation PutFileToStore {} {}", cd.getUuid(), filename);
            logger.error("error", e);
            //todo якщо IO помилка, то ми нічого не повинні повертати і закінчити завантаження файлу
            // return null
        }
        // Зберігаємо інформацію о файлі та привʼязуємо до користувача.
        Random random = new Random();
        long difference = random.nextInt(1000);  // затримка задля не повторення ID в базі
        Store storeitem = null;
        try {
            Thread.sleep(difference);
            // перевіряємо, чи є такий файл на диску
            if (!fileExist) {
                storeitem = SaveStoreItemInfo(null,destination, storeFileType, cd);
            } else {
                storeitem = GetStoreItemByFilenameByClientDetail(destination.getFileName().toString(), cd);
                // перевіряємо, чи є запис про такий файл в сховищі
                if (storeitem == null) { // є файл, але немає у сховищі запису про це
                    storeitem = SaveStoreItemInfo(null,destination, storeFileType, cd);
                }
            }
            historyService.saveHistory(History_DocumentCreate, " Завантажено файл: " + filename, cd.getUser());
        }
        catch(InterruptedException e)
        {
            logger.info("--------- Thread.sleep(difference) -> catch(InterruptedException e)");
            logger.info("Схоже ми тут ВИЛІТАЄМО!!!");
        }
        return storeitem.getUuid();

    }

    /// Видаляємо запис з бази
    public void DeleteStoreRecord(Store store) {
        storeRepositore.delete(store);
    }

    /// Видаляємо зі сховища
    public Boolean DeleteInStore(Store store) {
        // todo зробити видалення файлів у сховищі
        String sPatch = store.getFilepatch();
        logger.info("===== Видалення файла зі сховища {}",sPatch);
        try {
            // видалили файл
            FileUtils.forceDelete(FileUtils.getFile(sPatch));
        }
        catch (IOException e) {
            Path path = Paths.get(sPatch);
            boolean exists = Files.isRegularFile(path);
            if (exists) {
                logger.info("===== Store ID: {} Помилка - Файл існує. Patch {}", store.getId(), sPatch);
                return false;
            }
            else {
                logger.info("===== Store ID: {} Файл не існує. Patch {}", store.getId(), sPatch);
            }
           }
        storeRepositore.delete(store);
        return true;
    }


    public Store SaveStoreItemInfo(Store storeitem, Path destination, EStoreFileType eStoreFileType, Clientdetail cd) {


        if (storeitem == null) {
            storeitem = new Store();
            storeitem.setFilepatch(destination.toAbsolutePath().toString());
            storeitem.setStorefiletype(eStoreFileType);
            storeitem.setClientdetail(cd);
            storeitem.setFilename(destination.getFileName().toString());
            storeitem.setContentMimeType(filesService.GetMediatype(destination));
            storeitem.setFilelength(filesService.GetMediaLength(destination));

        } else  {
            storeitem.setContentMimeType(filesService.GetMediatype(destination));
            storeitem.setFilelength(filesService.GetMediaLength(destination));
        }
        try {
            storeRepositore.save(storeitem);
        } catch (Exception e)
        {
            logger.error("StoreFileImplementation -> SaveStoreItemInfo -> storeRepositore.save(storeitem)");
        }
        return storeitem;
    }

    public Store GetStoreItemByFilenameByClientDetail(String fileName, Clientdetail cd) {
        return storeRepositore.getByFilenameAndClientdetail(fileName,cd);
    }

    public Page GetStorePageItemType(int pageNumber, int pageCount, EStoreFileType eStoreFileType) {
        Pageable storePage = PageRequest.of(pageNumber, pageCount);
        Page page = storeRepositore.findByStorefiletype(storePage, eStoreFileType);
        return page;

    }

    public Store GetStoreByUUID(String storeUUID) {
        return storeRepositore.getByUuid(storeUUID);
    }

}
