package media.toloka.rfa.radio.store;
// https://paulcwarren.github.io/spring-content/refs/release/1.2.4/fs-index.html


import jakarta.servlet.http.HttpServletResponse;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.EStoreFileType;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.dropfile.service.FilesService;
import media.toloka.rfa.radio.model.Clientdetail;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
//import org.apache.commons.io.IOUtils

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.imgscalr.Scalr;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.imageio.ImageIO;

import static org.springframework.http.HttpHeaders.ACCEPT_RANGES;

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

    @GetMapping(value = {"/store/audio/{storeUUID}", "/store/audio/{storeUUID}/{fileName}"})
    public ResponseEntity<?> getStoreAudioToStream(
            @PathVariable("storeUUID") String storeUUID,
            @PathVariable(required = false) String fileName,
            @RequestHeader HttpHeaders headers
    ) {
        try {
            // 1. Шукаємо запис у базі даних
            Store storeRecord = storeService.GetStoreByUUID(storeUUID);
            if (storeRecord == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // 2. Перевіряємо фізичну наявність файлу на диску
            File file = new File(storeRecord.getFilepatch());
            if (!file.exists()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            Resource resource = new FileSystemResource(file);

            // 3. Визначаємо MIME-тип (дефолт audio/mpeg для mp3)
            String mimeType = storeRecord.getContentMimeType();
            if (mimeType == null || mimeType.isEmpty()) {
                mimeType = "audio/mpeg";
            }
            MediaType mediaType = MediaType.parseMediaType(mimeType);

            // 4. Перевіряємо, чи є в запиті заголовок Range від плеєра
            List<HttpRange> ranges = headers.getRange();
            if (ranges.isEmpty()) {
                // Якщо плеєр не просить шматки (Range відсутній) — віддаємо весь файл цілком (200 OK)
                logger.info("Віддача повного файлу для UUID: {}, розмір: {} байт", storeUUID, file.length());
                return ResponseEntity.ok()
                        .contentType(mediaType)
                        .contentLength(file.length())
                        .body(resource);
            }

            // 5. Якщо плеєр запитав конкретний Range (Частковий контент 206)
            long contentLength = resource.contentLength();
            HttpRange range = ranges.get(0); // Беремо перший діапазон

            long start = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);

            // ОБЧИСЛЕННЯ ДОВЖИНИ: віддаємо рівно стільки, скільки просить плеєр, без штучного ліміту в 1MB
            long rangeLength = end - start + 1;

            ResourceRegion region = new ResourceRegion(resource, start, rangeLength);

            logger.info("Стрімінг шматка для UUID: {}. Діапазон: bytes {}-{}/{}", storeUUID, start, end, contentLength);

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(mediaType)
                    .body(region);

        } catch (Exception e) {
            logger.error("Помилка стрімінгу аудіо для UUID {}: {}", storeUUID, e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


//    @GetMapping(value = {"/store/audio/{storeUUID}", "/store/audio/{storeUUID}/{fileName}"})
//    public ResponseEntity<ResourceRegion> getStoreAudioToStream(
//            @PathVariable("storeUUID") String storeUUID,
//            @PathVariable(required = false) String fileName,
//            @RequestHeader HttpHeaders headers
//    ) {
//        try {
//            Store storeRecord = storeService.GetStoreByUUID(storeUUID);
//            if (storeRecord == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//
//            File file = new File(storeRecord.getFilepatch());
//            if (!file.exists()) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//
//            Resource resource = new FileSystemResource(file);
//            ResourceRegion region = resourceRegion(resource, headers);
//
//            String mimeType = storeRecord.getContentMimeType();
//            if (mimeType == null || mimeType.isEmpty()) mimeType = "audio/mpeg";
//
//            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
//                    .contentType(MediaType.parseMediaType(mimeType))
//                    .body(region);
//        } catch (Exception e) {
//            logger.error("Streaming error: {}", e.getMessage());
//            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    private ResourceRegion resourceRegion(Resource resource, HttpHeaders headers) throws IOException {
//        long contentLength = resource.contentLength();
//        HttpRange range = headers.getRange().isEmpty() ? null : headers.getRange().get(0);
//        if (range != null) {
//            long start = range.getRangeStart(contentLength);
//            long end = range.getRangeEnd(contentLength);
//            long rangeLength = Math.min(1024 * 1024L, end - start + 1); // 1MB chunks
//            return new ResourceRegion(resource, start, rangeLength);
//        } else {
//            long rangeLength = Math.min(1024 * 1024L, contentLength);
//            return new ResourceRegion(resource, 0, rangeLength);
//        }
//    }

    //тимчасово продублював для верифікації RSS XML подкасту
    @GetMapping(value = "/podcast/audio/{storeUUID}/{fileName}")
    public ResponseEntity<?> getStoreAudioToStreamWFN(
//    public ResponseEntity<ResourceRegion> getStoreAudioToStreamWFN(
            @PathVariable("storeUUID") String storeUUID,
            @PathVariable String fileName,
            @RequestHeader HttpHeaders headers
    ) {
        return getStoreAudioToStream(storeUUID, fileName, headers);
    }

    @GetMapping(value = "/store/img/{clientUUID}/{fileName}",
            produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_GIF_VALUE})
    public @ResponseBody byte[] getStoreImage(
            @PathVariable String clientUUID,
            @PathVariable String fileName,
            Model model ) {
        Clientdetail cd = clientService.GetClientDetailByUuid(clientUUID);
//        http://localhost:8080/store/e2f9b0e6-73b5-4fcf-b249-f1e82d42a689/123.jpg
        // todo Прибрати роботу з ресурсами і зробити звичайну роботу з файлами.
        String ifile = filesService.GetBaseClientDirectory(cd)+"/"+fileName;
        InputStream is;
        try {
            is = new FileInputStream(new File(ifile));
            if (is == null) {
                return new byte[0];
            }
            byte[] buffer = is.readAllBytes();
            return buffer;
        } catch (FileNotFoundException e) {
            logger.info("getStoreAudio: Йой! FileNotFoundException! {}",ifile);
        } catch (IOException e) {
            logger.info("==================================== getStoreImage IOException");
            logger.info("Проблеми з файлом: {}",ifile);
            e.printStackTrace();
            return null;
        }
        return null;
    }

    @GetMapping(value = "/store/thrumbal/{storeUUID}/{fileName}",
            produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_GIF_VALUE})
    public @ResponseBody byte[] getStoreThrumbal(
            @PathVariable String storeUUID,
            @PathVariable String fileName,
            Model model ) {
        // https://medium.com/@asadise/create-thumbnail-for-an-image-in-spring-framework-49776c873ea1
        // http://localhost:8080/store/thrumbal/e2f9b0e6-73b5-4fcf-b249-f1e82d42a689/123.jpg
        InputStream is;

        OutputStream os;

        BufferedImage thumbImg = null;
        BufferedImage img;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Store storeRecord;
        storeRecord = storeService.GetStoreByUUID(storeUUID);
        try {
//            storeRecord = storeService.GetStoreByUUID(storeUUID);
            String ifile = storeRecord.getFilepatch();
            is = new FileInputStream(new File(ifile));
            if (is == null) {
                return new byte[0];
            }
             img = ImageIO.read(is);
        } catch (IOException e) {
            logger.info("==================================== getStoreImage IOException");
            logger.info("Проблеми з файлом: {}",storeRecord.getFilepatch());
//            logger.info("Проблеми з файлом: {}",ifile);
//
//            e.printStackTrace();
            return null;
        }
        thumbImg = Scalr.resize(img, Scalr.Method.AUTOMATIC, Scalr.Mode.AUTOMATIC, 320, Scalr.OP_ANTIALIAS);
        try {
            ImageIO.write(thumbImg, FilenameUtils.getExtension(storeRecord.getFilename()), baos);
        } catch (IOException e) {
            logger.info("==================================== getStoreImage IOException");
            logger.info("Проблеми з файлом: {}",storeRecord.getFilepatch());

//            e.printStackTrace();
            return null;
        }
        byte[] bytes = baos.toByteArray();
        return bytes;
    }

    @GetMapping(value = "/store/thrumbal/w/{width}/{storeUUID}/{fileName}",
            produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_GIF_VALUE})
    public @ResponseBody byte[] getStoreThrumbalWidth(
            @PathVariable String storeUUID,
            @PathVariable int width,
            @PathVariable String fileName,
            Model model ) {
        // https://medium.com/@asadise/create-thumbnail-for-an-image-in-spring-framework-49776c873ea1
        // http://localhost:8080/store/thrumbal/e2f9b0e6-73b5-4fcf-b249-f1e82d42a689/123.jpg
        Store storeRecord = storeService.GetStoreByUUID(storeUUID);

        Clientdetail cd = clientService.GetClientDetailByUuid(storeRecord.getClientdetail().getUuid());
//        http://localhost:8080/store/e2f9b0e6-73b5-4fcf-b249-f1e82d42a689/123.jpg
        InputStream is;

        OutputStream os;

        BufferedImage thumbImg = null;
        BufferedImage img;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // беремо шлях до файлу зі сторе
        String filePatch = storeRecord.getFilepatch();
        try {
            is = new FileInputStream(new File(filePatch));
            if (is == null) {
                return new byte[0];
            }
            img = ImageIO.read(is);
        } catch (IOException e) {
            logger.info("==================================== getStoreImage IOException");
            logger.info("Проблеми з файлом: {}",storeRecord.getFilepatch());
//            e.printStackTrace();
            return null;
        }
        thumbImg = Scalr.resize(img, Scalr.Method.AUTOMATIC, Scalr.Mode.AUTOMATIC, width, Scalr.OP_ANTIALIAS);
        try {
            ImageIO.write(thumbImg, FilenameUtils.getExtension(fileName), baos);
        } catch (IOException e) {
            logger.info("==================================== getStoreImage IOException");
            logger.info("Проблеми з файлом: {}",storeRecord.getFilepatch());
//            e.printStackTrace();
            return null;
        }
        byte[] bytes = baos.toByteArray();
        return bytes;
    }

// ==================== Пробуємо завантажити документ

    @GetMapping(value = "/store/document/{storeUUID}"     )
    public @ResponseBody byte[] getStoreDoc(
            @PathVariable String storeUUID,
//            @PathVariable String fileName,
            HttpServletResponse response,
            Model model ) {
        Store storeObject = storeService.GetStoreByUUID(storeUUID);
//        http://localhost:8080/store/e2f9b0e6-73b5-4fcf-b249-f1e82d42a689/123.jpg
        response.setContentType(storeObject.getContentMimeType());
        response.setContentLength(storeObject.getFilelength().intValue());
        String ifile = storeObject.getFilepatch();
        InputStream is;
        try {
            is = new FileInputStream(new File(ifile));
            if (is == null) {
                return new byte[0];
            }
            byte[] buffer = is.readAllBytes();
            return buffer;
        } catch (FileNotFoundException e) {
            logger.info("getStoreAudio: Йой! FileNotFoundException! {}",ifile);
        } catch (IOException e) {
            logger.info("==================================== getStoreImage IOException");
            logger.info("Проблеми з файлом: {}",ifile);
            e.printStackTrace();
            return null;
        }
        return null;
    }

    /// вигрібаємо зі сховища встановлюючи тип контенту.
    /// використовується для відображення на сайті та для завантаження з сайту.
    @GetMapping(value = "/store/content/{storeUUID}")
    public @ResponseBody byte[] getStoreContent(
            @PathVariable String storeUUID,
            HttpServletResponse response,
            Model model ) {
        Store storeObject = storeService.GetStoreByUUID(storeUUID);
        if (storeObject == null) {
            logger.info("getStoreContent: UUID не знайдено у сховищі {}",storeUUID);
            return null;
        }
//        http://localhost:8080/store/e2f9b0e6-73b5-4fcf-b249-f1e82d42a689/123.jpg
        response.setContentType(storeObject.getContentMimeType());
        response.setContentLength(storeObject.getFilelength().intValue());
        String ifile = storeObject.getFilepatch();
        InputStream is;
        try {
            is = new FileInputStream(new File(ifile));
            if (is == null) {
                return new byte[0];
            }
            byte[] buffer = is.readAllBytes();
            return buffer;
        } catch (FileNotFoundException e) {
            logger.info("getStoreContent: Йой! FileNotFoundException! {}",ifile);
        } catch (IOException e) {
            logger.info("==================================== getStoreContent IOException");
            logger.info("Проблеми з файлом: {}",ifile);
        }
        return null;
    }

    // вигрібаємо зі сховища встановлюючи тип контенту.
    // використовується для відображення на сайті та для завантаження з сайту.
    @GetMapping(value = "/store/content/og/{storeUUID}/{fn}")
    public @ResponseBody byte[] getStoreOgContent(
            @PathVariable String storeUUID,
            @PathVariable String fn,
            HttpServletResponse response,
            Model model ) {
        Store storeObject = storeService.GetStoreByUUID(storeUUID);
//        http://localhost:8080/store/e2f9b0e6-73b5-4fcf-b249-f1e82d42a689/123.jpg
        response.setContentType(storeObject.getContentMimeType());
        response.setContentLength(storeObject.getFilelength().intValue());
        String ifile = storeObject.getFilepatch();
        InputStream is;
        try {
            is = new FileInputStream(new File(ifile));
            if (is == null) {
                return new byte[0];
            }
            byte[] buffer = is.readAllBytes();
            return buffer;
        } catch (FileNotFoundException e) {
            logger.info("getStoreContent: Йой! FileNotFoundException! {}",ifile);
        } catch (IOException e) {
            logger.info("==================================== getStoreContent IOException");
            logger.info("Проблеми з файлом: {}",ifile);
//            e.printStackTrace();
//            return null;
        }
        return null;
    }



}
