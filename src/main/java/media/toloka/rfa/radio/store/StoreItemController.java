package media.toloka.rfa.radio.store;

import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.EStoreFileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Profile("Front")
@RestController
@RequestMapping("/api/store")
@Tag(name = "Універсальна процедура завантаження файлів", description = "Завантаження файлів з параметром типу файлу.")
public class StoreItemController {

    private final Logger logger = LoggerFactory.getLogger(StoreItemController.class);

    @Autowired
    private ClientService clientService;

    @Autowired
    private StoreService storeService;

    @PostMapping("/upload")
    public ResponseEntity<?> universalUpload(
            @Parameter(description = "файл або файли, які передаємо") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Тип файла у сховищі") @RequestParam(value = "type", defaultValue = "STORE_FILE") String type) {

        // 1. Отримання поточного клієнта
        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
        }

        // 2. Перевірка прав на завантаження
        if (!clientService.ClientCanDownloadFile(cd)) {
            logger.warn("Client {} does not have permission to upload files", cd.getUuid());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        try {
            // 3. Визначення типу (Enum)
            EStoreFileType fileType;
            try {
                fileType = EStoreFileType.valueOf(type);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid store file type: {}", type);
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid file type"));
            }

            // 4. Завантаження через StoreService (включає транслітерацію та розміри)
            String storeUUID = storeService.PutFileToStore(file.getInputStream(), file.getOriginalFilename(), cd, fileType);

            logger.info("Universal upload success: type={}, filename={}, uuid={}", fileType, file.getOriginalFilename(), storeUUID);

            // 5. Повернення стандартизованої відповіді
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "uuid", storeUUID,
                    "url", "/store/content/" + storeUUID,
                    "filename", file.getOriginalFilename()
            ));
        } catch (Exception e) {
            logger.error("Universal upload error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
