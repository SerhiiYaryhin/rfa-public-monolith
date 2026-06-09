package media.toloka.rfa.radio.admin.controllerREST;

import media.toloka.rfa.radio.admin.service.AdminService;
import media.toloka.rfa.radio.model.Clientaddress;
import media.toloka.rfa.radio.model.enumerate.EDocumentStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/1.0/admin/approval")
public class AdminApprovalRESTController {

    @Autowired
    private AdminService adminService;

    @PostMapping("/address/{id}/approve")
    public ResponseEntity<?> approveAddress(@PathVariable Long id) {
        Clientaddress address = adminService.GetClientAddress(id);
        if (address == null) return ResponseEntity.notFound().build();
        
        address.setApruve(true);
        adminService.SaveClientAddress(address);
        return ResponseEntity.ok(Map.of("success", true, "message", "Адресу схвалено"));
    }

    @PostMapping("/document/{id}/status")
    public ResponseEntity<?> updateDocumentStatus(
            @PathVariable Long id, 
            @RequestParam EDocumentStatus status, 
            @RequestParam(required = false) String comment) {
        
        adminService.updateDocumentStatus(id, status, comment);
        String message = status == EDocumentStatus.STATUS_APPROVED ? "Документ схвалено" : "Статус документа змінено";
        return ResponseEntity.ok(Map.of("success", true, "message", message));
    }
}
