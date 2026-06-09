package media.toloka.rfa.radio.admin.dto;

import lombok.Builder;
import lombok.Data;
import media.toloka.rfa.radio.model.Clientaddress;
import media.toloka.rfa.radio.model.Documents;

import java.util.List;

@Data
@Builder
public class PendingApprovalDTO {
    private Long clientId;
    private String userEmail;
    private String fullName;
    private String firmName;
    private List<Clientaddress> pendingAddresses;
    private List<Documents> pendingDocuments;
    private int totalPending;
}
