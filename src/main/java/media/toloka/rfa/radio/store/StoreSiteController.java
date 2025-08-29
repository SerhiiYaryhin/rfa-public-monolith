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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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


    @GetMapping(value = "/store/audio/{storeUUID}",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    public ResponseEntity<StreamingResponseBody> getStoreAudioToStream(
            @PathVariable("storeUUID") String storeUUID,
            @RequestHeader(value = "Range", required = false) String rangeHeader
    ) {
        // https://www.codeproject.com/Articles/5341970/Streaming-Media-Files-in-Spring-Boot-Web-Applicati
        try
        {
            StreamingResponseBody responseStream;
            Store storeRecord = storeService.GetStoreByUUID(storeUUID);
//            String filePathString = filesService.GetBaseClientDirectory(storeRecord.getClientdetail())
//                    + "/upload/" + storeRecord.getFilename();
            String filePathString = storeRecord.getFilepatch();
//            String filePathString = "<Place your MP4 file full path here.>";
            Path filePath = Paths.get(filePathString);
            Long fileSize = Files.size(filePath);
            byte[] buffer = new byte[1024];
            final HttpHeaders responseHeaders = new HttpHeaders();

            if (rangeHeader == null)
            {
                responseHeaders.add("Content-Type", storeRecord.getContentMimeType());
//                responseHeaders.add("Content-Length", fileSize.toString());
                responseHeaders.add("Content-Length", storeRecord.getFilelength().toString());
                responseStream = os -> {
                    RandomAccessFile file = new RandomAccessFile(filePathString, "r");
                    try (file)
                    {
                        long pos = 0;
                        file.seek(pos);
                        while (pos < fileSize - 1)
                        {
                            file.read(buffer);
                            os.write(buffer);
                            pos += buffer.length;
                        }
                        os.flush();
                    } catch (Exception e) {
                        // todo додати обробку
                    }
                };

                return new ResponseEntity<StreamingResponseBody>
                        (responseStream, responseHeaders, HttpStatus.OK);
            }

            String[] ranges = rangeHeader.split("-");
            Long rangeStart = Long.parseLong(ranges[0].substring(6));
            Long rangeEnd;
            if (ranges.length > 1)
            {
                rangeEnd = Long.parseLong(ranges[1]);
            }
            else
            {
                rangeEnd = fileSize - 1;
            }

            if (fileSize < rangeEnd)
            {
                rangeEnd = fileSize - 1;
            }

            String contentLength = String.valueOf((rangeEnd - rangeStart) + 1);
            responseHeaders.add("Content-Type", storeRecord.getContentMimeType());
            //responseHeaders.add("Content-Type", "video/mp4");
            responseHeaders.add("Content-Length", contentLength);
            responseHeaders.add("Accept-Ranges", "bytes");
            responseHeaders.add("Content-Range", "bytes" + " " +
                    rangeStart + "-" + rangeEnd + "/" + fileSize);
            final Long _rangeEnd = rangeEnd;
            responseStream = os -> {
                RandomAccessFile file = new RandomAccessFile(filePathString, "r");
                try (file)
                {
                    long pos = rangeStart;
                    file.seek(pos);
                    while (pos < _rangeEnd)
                    {
                        file.read(buffer);
                        os.write(buffer);
                        pos += buffer.length;
                    }
                    os.flush();
                }
                catch (Exception e) {
                    // **ЛОГУЙТЕ ВИКЛЮЧЕННЯ!!!**
                    System.err.println("Помилка під час потокової передачі файлу: " + e.getMessage());
//                    e.printStackTrace(); // Для налагодження
                }
            };

            return new ResponseEntity<StreamingResponseBody>
                    (responseStream, responseHeaders, HttpStatus.PARTIAL_CONTENT);
        }
        catch (FileNotFoundException e)
        {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        catch (IOException e)
        {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    //тимчасово продублював для верифікації RSS XML подкасту
    @GetMapping(value = "/podcast/audio/{storeUUID}/{fileName}",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    public ResponseEntity<StreamingResponseBody> getStoreAudioToStreamWFN(
            @PathVariable("storeUUID") String storeUUID,
            @PathVariable String fileName,
            @RequestHeader(value = "Range", required = false) String rangeHeader
    ) {
        // https://www.codeproject.com/Articles/5341970/Streaming-Media-Files-in-Spring-Boot-Web-Applicati
        try
        {
            StreamingResponseBody responseStream;
            Store storeRecord = storeService.GetStoreByUUID(storeUUID);
//            String filePathString = filesService.GetBaseClientDirectory(storeRecord.getClientdetail())
//                    + "/upload/" + storeRecord.getFilename();
            String filePathString = storeRecord.getFilepatch();
//            String filePathString = "<Place your MP4 file full path here.>";
            Path filePath = Paths.get(filePathString);
            Long fileSize = Files.size(filePath);
            byte[] buffer = new byte[1024];
            final HttpHeaders responseHeaders = new HttpHeaders();

            if (rangeHeader == null)
            {
                responseHeaders.add("Content-Type", storeRecord.getContentMimeType());
//                responseHeaders.add("Content-Length", fileSize.toString());
                responseHeaders.add("Content-Length", storeRecord.getFilelength().toString());
                responseStream = os -> {
                    RandomAccessFile file = new RandomAccessFile(filePathString, "r");
                    try (file)
                    {
                        long pos = 0;
                        file.seek(pos);
                        while (pos < fileSize - 1)
                        {
                            file.read(buffer);
                            os.write(buffer);
                            pos += buffer.length;
                        }
                        os.flush();
                    } catch (Exception e) {
                        // todo додати обробку
                    }
                };

                return new ResponseEntity<StreamingResponseBody>
                        (responseStream, responseHeaders, HttpStatus.OK);
            }

            String[] ranges = rangeHeader.split("-");
            Long rangeStart = Long.parseLong(ranges[0].substring(6));
            Long rangeEnd;
            if (ranges.length > 1)
            {
                rangeEnd = Long.parseLong(ranges[1]);
            }
            else
            {
                rangeEnd = fileSize - 1;
            }

            if (fileSize < rangeEnd)
            {
                rangeEnd = fileSize - 1;
            }

            String contentLength = String.valueOf((rangeEnd - rangeStart) + 1);
            responseHeaders.add("Content-Type", storeRecord.getContentMimeType());
            //responseHeaders.add("Content-Type", "video/mp4");
            responseHeaders.add("Content-Length", contentLength);
            responseHeaders.add("Accept-Ranges", "bytes");
            responseHeaders.add("Content-Range", "bytes" + " " +
                    rangeStart + "-" + rangeEnd + "/" + fileSize);
            final Long _rangeEnd = rangeEnd;
            responseStream = os -> {
                RandomAccessFile file = new RandomAccessFile(filePathString, "r");
                try (file)
                {
                    long pos = rangeStart;
                    file.seek(pos);
                    while (pos < _rangeEnd)
                    {
                        file.read(buffer);
                        os.write(buffer);
                        pos += buffer.length;
                    }
                    os.flush();
                }
                catch (Exception e) {}
            };

            return new ResponseEntity<StreamingResponseBody>
                    (responseStream, responseHeaders, HttpStatus.PARTIAL_CONTENT);
        }
        catch (FileNotFoundException e)
        {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        catch (IOException e)
        {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
