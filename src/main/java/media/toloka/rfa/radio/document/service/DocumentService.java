package media.toloka.rfa.radio.document.service;

import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.model.enumerate.EDocumentStatus;
import media.toloka.rfa.radio.repository.DocumentRepository;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.Store;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import media.toloka.rfa.radio.model.Documents;

import java.nio.file.Path;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DocumentService {

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

//    public List<Documents> listDocumentsFromClientdetail(Clientdetail cl) {
//        return documentRepository.findDocumentByClientdetail(cl) ;
//        return cl.getDocumentslist();
//    }
    public List<Documents> listDocumentsByClientdetail(Clientdetail cl) {
        List<Documents> ld = documentRepository.findByClientdetail(cl) ;
        return ld;
    }

    public void SaveDocumentUploadInfo(String storeUUID, Clientdetail cd, Store store) {
        Documents doc = new Documents();
        doc.setStatus(EDocumentStatus.STATUS_LOADED);
//        doc.setPathToDocument(destination.getFileName().toString());
        doc.setClientdetail(cd);
        doc.setStoreuuid(storeUUID);
        doc.setStore(store);
        documentRepository.save(doc);
    }

    public List<Documents> GetUnApruvedDocumentsOrderLoaddate() {
//        return documentRepository.findByStatusLessThan(EDocumentStatus.STATUS_APPROVED);
//        findByStatusNot
        return documentRepository.findByStatusNot(EDocumentStatus.STATUS_APPROVED);
    }

    public Documents GetDocumentByUUID(String uuid) {
        Documents doc = documentRepository.getByUuid(uuid);
        return doc;
    }
}
