package media.toloka.rfa.radio.store;

import jakarta.servlet.http.HttpServletResponse;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.dropfile.service.FilesService;
import media.toloka.rfa.security.model.Users;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

@CrossOrigin
@Profile("Front")
@Controller
public class StoreSiteController  {

    final Logger logger = LoggerFactory.getLogger(StoreSiteController.class);

    @Autowired
    private ClientService clientService;

    @Autowired
    private FilesService filesService;

    @Autowired
    private CreaterService createrService;

    @Autowired
    private StoreService storeService;

    /**
     * Спільний метод для безпечної передачі файлів через стрім
     */
    private ResponseEntity<StreamingResponseBody> streamFile(Store store, Users user) {
        if (store == null) return ResponseEntity.notFound().build();

        if (!storeService.canUserAccessStoreItem(store, user)) {
            logger.warn("Спроба несанкціонованого доступу до файлу {} користувачем {}", 
                store.getUuid(), user != null ? user.getEmail() : "anonymous");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        File file = new File(store.getFilepatch());
        if (!file.exists()) return ResponseEntity.notFound().build();

        String mimeType = store.getContentMimeType();
        if (mimeType == null || mimeType.isEmpty()) mimeType = "application/octet-stream";

        String finalMimeType = mimeType;
        StreamingResponseBody responseBody = outputStream -> {
            try (InputStream inputStream = new FileInputStream(file)) {
                IOUtils.copy(inputStream, outputStream);
                outputStream.flush();
            } catch (IOException e) {
                logger.error("Помилка при стрімінгу файлу {}: {}", store.getUuid(), e.getMessage());
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, finalMimeType)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.length()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + store.getFilename() + "\"")
                .body(responseBody);
    }

    @GetMapping(value = {"/store/audio/{storeUUID}", "/store/audio/{storeUUID}/{fileName:.+}"})
    public ResponseEntity<Resource> getStoreAudioToStream(
            @PathVariable("storeUUID") String storeUUID,
            @PathVariable(required = false) String fileName,
            @RequestHeader HttpHeaders headers
    ) {
        try {
            Users user = clientService.GetCurrentUser();
            Store storeRecord = storeService.GetStoreByUUID(storeUUID);
            
            if (storeRecord == null) return ResponseEntity.notFound().build();

            // Перевірка прав доступу
            if (!storeService.canUserAccessStoreItem(storeRecord, user)) {
                logger.warn("Несанкціонована спроба стрімінгу аудіо {}: {}", storeUUID, user != null ? user.getEmail() : "anonymous");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            File file = new File(storeRecord.getFilepatch());
            if (!file.exists()) return ResponseEntity.notFound().build();

            String mimeType = storeRecord.getContentMimeType();
            if (mimeType == null || mimeType.isEmpty()) mimeType = "audio/mpeg";
            
            Resource resource = new FileSystemResource(file);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(resource);

        } catch (Exception e) {
            logger.error("Помилка стрімінгу аудіо для UUID {}: {}", storeUUID, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/podcast/audio/{storeUUID}/{fileName:.+}")
    public ResponseEntity<?> getStoreAudioToStreamWFN(
            @PathVariable("storeUUID") String storeUUID,
            @PathVariable String fileName,
            @RequestHeader HttpHeaders headers
    ) {
        return getStoreAudioToStream(storeUUID, fileName, headers);
    }

    @GetMapping(value = "/store/thrumbal/{storeUUID}/{fileName}")
    public ResponseEntity<StreamingResponseBody> getStoreThrumbal(
            @PathVariable String storeUUID,
            @PathVariable String fileName) {
        
        Users user = clientService.GetCurrentUser();
        Store storeRecord = storeService.GetStoreByUUID(storeUUID);
        
        if (storeRecord == null) return ResponseEntity.notFound().build();
        if (!storeService.canUserAccessStoreItem(storeRecord, user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        StreamingResponseBody responseBody = outputStream -> {
            try (InputStream is = new FileInputStream(new File(storeRecord.getFilepatch()))) {
                BufferedImage img = ImageIO.read(is);
                if (img != null) {
                    BufferedImage thumbImg = Scalr.resize(img, Scalr.Method.AUTOMATIC, Scalr.Mode.AUTOMATIC, 320, Scalr.OP_ANTIALIAS);
                    String ext = FilenameUtils.getExtension(storeRecord.getFilename());
                    ImageIO.write(thumbImg, (ext != null && !ext.isEmpty()) ? ext : "jpg", outputStream);
                    outputStream.flush();
                }
            } catch (IOException e) {
                logger.error("Помилка генерації мініатюри для {}: {}", storeUUID, e.getMessage());
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(responseBody);
    }

    @GetMapping(value = "/store/thrumbal/w/{width}/{storeUUID}/{fileName}")
    public ResponseEntity<StreamingResponseBody> getStoreThrumbalWidth(
            @PathVariable String storeUUID,
            @PathVariable int width,
            @PathVariable String fileName) {
        
        Users user = clientService.GetCurrentUser();
        Store storeRecord = storeService.GetStoreByUUID(storeUUID);
        
        if (storeRecord == null) return ResponseEntity.notFound().build();
        if (!storeService.canUserAccessStoreItem(storeRecord, user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        StreamingResponseBody responseBody = outputStream -> {
            try (InputStream is = new FileInputStream(new File(storeRecord.getFilepatch()))) {
                BufferedImage img = ImageIO.read(is);
                if (img != null) {
                    BufferedImage thumbImg = Scalr.resize(img, Scalr.Method.AUTOMATIC, Scalr.Mode.AUTOMATIC, width, Scalr.OP_ANTIALIAS);
                    String ext = FilenameUtils.getExtension(fileName);
                    ImageIO.write(thumbImg, (ext != null && !ext.isEmpty()) ? ext : "jpg", outputStream);
                    outputStream.flush();
                }
            } catch (IOException e) {
                logger.error("Помилка генерації мініатюри (w={}) для {}: {}", width, storeUUID, e.getMessage());
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(responseBody);
    }

    @GetMapping(value = "/store/document/{storeUUID}")
    public ResponseEntity<StreamingResponseBody> getStoreDoc(@PathVariable String storeUUID) {
        Users user = clientService.GetCurrentUser();
        Store store = storeService.GetStoreByUUID(storeUUID);
        return streamFile(store, user);
    }

    @GetMapping(value = "/store/content/{storeUUID}")
    public ResponseEntity<StreamingResponseBody> getStoreContent(@PathVariable String storeUUID) {
        Users user = clientService.GetCurrentUser();
        Store store = storeService.GetStoreByUUID(storeUUID);
        return streamFile(store, user);
    }

    @GetMapping(value = "/store/content/og/{storeUUID}/{fn}")
    public ResponseEntity<StreamingResponseBody> getStoreOgContent(
            @PathVariable String storeUUID,
            @PathVariable String fn) {
        Users user = clientService.GetCurrentUser();
        Store store = storeService.GetStoreByUUID(storeUUID);
        return streamFile(store, user);
    }
}
