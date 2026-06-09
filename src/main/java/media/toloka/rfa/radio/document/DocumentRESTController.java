package media.toloka.rfa.radio.document;

import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.document.service.DocumentService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.security.model.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/1.0/user/document")
public class DocumentRESTController {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private StoreService storeService;

    @PostMapping("/link-store")
    public ResponseEntity<?> linkStoreToDocument(@RequestParam String storeUuid, @RequestParam(required = false) String type) {
        Users user = clientService.GetCurrentUser();
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        Clientdetail cd = clientService.GetClientDetailByUser(user);
        Store store = storeService.GetStoreByUUID(storeUuid);

        if (store == null || !store.getClientdetail().getUuid().equals(cd.getUuid())) {
            return ResponseEntity.badRequest().body("Invalid store file");
        }

        documentService.SaveDocumentUploadInfo(storeUuid, cd, store);
        
        return ResponseEntity.ok(Map.of("success", true, "message", "Документ успішно додано"));
    }
}
