package media.toloka.rfa.radio.document.service;

import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.enumerate.EDocumentStatus;
import media.toloka.rfa.radio.repository.DocumentRepository;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import media.toloka.rfa.radio.model.Documents;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;

@Service
@Transactional
public class DocumentService {

    private final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ClientService clientService;

    public Documents GetDocument(Long id) {
        return documentRepository.getById(id);
    }

    public void saveDocument(Documents document) {
        documentRepository.save(document);
    }

    public List<Documents> listDocumentsByClientdetail(Clientdetail cl) {
        return documentRepository.findByClientdetail(cl);
    }

    public void SaveDocumentUploadInfo(String storeUUID, Clientdetail cd, Store store) {
        logger.info("Saving document info for user {}: storeUuid={}", cd.getUuid(), storeUUID);
        
        Documents doc = new Documents();
        doc.setStatus(EDocumentStatus.STATUS_LOADED);
        doc.setClientdetail(cd);
        doc.setStoreuuid(storeUUID);
        doc.setStore(store);
        doc.setDocumenttype(store != null ? store.getFilename() : "Документ");
        
        documentRepository.save(doc);
        
        // Оновлюємо колекцію в об'єкті cd
        if (cd.getDocumentslist() == null) {
            cd.setDocumentslist(new java.util.ArrayList<>());
        }
        cd.getDocumentslist().add(doc);
        
        logger.info("Document record created and linked with ID: {}", doc.getId());
    }

    public List<Documents> GetUnApruvedDocumentsOrderLoaddate() {
        return documentRepository.findByStatusNot(EDocumentStatus.STATUS_APPROVED);
    }

    public Documents GetDocumentByUUID(String uuid) {
        return documentRepository.getByUuid(uuid);
    }
}
