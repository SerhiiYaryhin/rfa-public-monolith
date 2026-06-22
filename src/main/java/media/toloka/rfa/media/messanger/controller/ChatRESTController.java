package media.toloka.rfa.media.messanger.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.store.Service.StoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import static media.toloka.rfa.radio.store.model.EStoreFileType.STORE_CHATIMAGE;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRESTController {

    private final ClientService clientService;
    private final StoreService storeService;

    /**
     * Завантаження зображення для чату.
     */
    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadChatImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Файл порожній"));
        }

        Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
        if (cd == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Авторизуйтесь для завантаження"));
        }

        if (!clientService.ClientCanDownloadFile(cd)) {
            return ResponseEntity.status(403).body(Map.of("error", "Немає прав на завантаження"));
        }

        try {
            log.info("Чат: Завантаження зображення {} від користувача {}", file.getOriginalFilename(), cd.getUuid());
            String storeUUID = storeService.PutFileToStore(file.getInputStream(), file.getOriginalFilename(), cd, STORE_CHATIMAGE);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "uuid", storeUUID,
                "url", "/store/content/" + storeUUID
            ));
        } catch (IOException e) {
            log.error("Помилка збереження зображення чату", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Помилка сервера при збереженні"));
        }
    }
}
