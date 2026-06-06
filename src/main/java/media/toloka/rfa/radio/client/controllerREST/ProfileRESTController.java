package media.toloka.rfa.radio.client.controllerREST;

import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.Clientaddress;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Documents;
import media.toloka.rfa.radio.model.enumerate.EDocumentStatus;
import media.toloka.rfa.security.model.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/1.0/user/profile")
public class ProfileRESTController {

    @Autowired
    private ClientService clientService;

    @PostMapping("/confirm-info")
    public ResponseEntity<?> confirmInfo() {
        Users user = clientService.GetCurrentUser();
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        Clientdetail cd = clientService.GetClientDetailByUser(user);
        cd.setConfirminfo(true);
        clientService.SaveClientDetail(cd);

        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/verification-status")
    public ResponseEntity<?> getVerificationStatus() {
        Users user = clientService.GetCurrentUser();
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        Clientdetail cd = clientService.GetClientDetailByUser(user);
        
        List<Clientaddress> addresses = clientService.GetClientAddressList(cd);
        boolean addressApproved = addresses.stream().anyMatch(Clientaddress::getApruve);
        
        List<Documents> docs = cd.getDocumentslist();
        boolean docApproved = docs.stream().anyMatch(d -> d.getStatus() == EDocumentStatus.STATUS_APPROVED);

        return ResponseEntity.ok(Map.of(
            "confirmInfo", cd.getConfirminfo(),
            "addressApproved", addressApproved,
            "docApproved", docApproved,
            "allApproved", cd.getConfirminfo() && addressApproved && docApproved
        ));
    }
}
