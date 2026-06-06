package media.toloka.rfa.radio.admin.service;

import media.toloka.rfa.radio.admin.dto.PendingApprovalDTO;
import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.contract.service.ContractService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.document.service.DocumentService;
import media.toloka.rfa.radio.history.service.HistoryService;
import media.toloka.rfa.radio.model.*;
import media.toloka.rfa.radio.model.enumerate.EDocumentStatus;
import media.toloka.rfa.radio.post.service.PostService;
import media.toloka.rfa.radio.station.service.StationService;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    @Autowired
    private ClientService clientService;

    @Autowired
    private CreaterService createrService;

    @Autowired
    private PostService postService;

    @Autowired
    private ContractService contractService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private StationService stationService;

    @Autowired
    private StoreService storeService;

    final Logger logger = LoggerFactory.getLogger(AdminService.class);

    public List<Post> GetNotApruvePosts() {
        return postService.GetListPostByApruve(false);
    }

    public List<Post> GetAllPosts() {
        return postService.GetAllPosts();
    }

    public Post GetPostById(Long postId) {
        return postService.GetPostById(postId);
    }

    public void SavePost(Post post) {
        postService.SavePost(post);
    }

    public List<Users> GetAllUsers() {
        return clientService.GetAllUsers();
    }

    public List<Users> GetSearchUsers(String template) {
        if (template == null) {
            return clientService.GetAllUsers();
        }
        return clientService.GetSearchUsers(template);
    }

    @Autowired
    private media.toloka.rfa.radio.repository.UserRepository userRepository;

    public org.springframework.data.domain.Page<Users> getUsersPage(int page, int size, String query, String filter) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("id").descending());
        
        if (filter != null) {
            if (filter.equals("pending_docs")) {
                return userRepository.findUsersWithPendingDocuments(pageable);
            } else if (filter.equals("pending_address")) {
                return userRepository.findUsersWithPendingAddresses(pageable);
            }
        }
        
        if (query == null || query.isEmpty()) {
            return userRepository.findAll(pageable);
        }
        return userRepository.findUsersBySearchTemplate(query, pageable);
    }

    public List<Documents> GetNotApruvedDocuments() {
        return documentService.GetUnApruvedDocumentsOrderLoaddate();
    }

    public List<Clientaddress> GetNotApruvedAddresses() {
        return clientService.GetUnApruvedDocumentsOrderLoaddate();
    }

    public Clientaddress GetClientAddress(Long idAddress) {
        return clientService.GetClientAddressById(idAddress);
    }

    public void SaveClientAddress(Clientaddress clientaddress) {
        clientService.SaveAddress(clientaddress);
    }

    public void updateDocumentStatus(Long id, EDocumentStatus status, String adminComment) {
        Documents doc = documentService.GetDocument(id);
        if (doc != null) {
            doc.setStatus(status);
            if (adminComment != null) doc.setAdminComment(adminComment);
            documentService.saveDocument(doc);
        }
    }

    public List<PendingApprovalDTO> GetPendingApprovals() {
        Map<Long, PendingApprovalDTO> userMap = new HashMap<>();

        // 1. Обробляємо адреси
        List<Clientaddress> pendingAddresses = GetNotApruvedAddresses();
        for (Clientaddress addr : pendingAddresses) {
            Clientdetail cd = addr.getClientdetail();
            PendingApprovalDTO dto = getOrCreateDTO(userMap, cd);
            dto.getPendingAddresses().add(addr);
        }

        // 2. Обробляємо документи
        List<Documents> pendingDocs = GetNotApruvedDocuments();
        for (Documents doc : pendingDocs) {
            Clientdetail cd = doc.getClientdetail();
            PendingApprovalDTO dto = getOrCreateDTO(userMap, cd);
            dto.getPendingDocuments().add(doc);
        }

        List<PendingApprovalDTO> approvals = new ArrayList<>(userMap.values());
        approvals.forEach(a -> a.setTotalPending(a.getPendingAddresses().size() + a.getPendingDocuments().size()));
        
        return approvals;
    }

    private PendingApprovalDTO getOrCreateDTO(Map<Long, PendingApprovalDTO> userMap, Clientdetail cd) {
        return userMap.computeIfAbsent(cd.getId(), id -> PendingApprovalDTO.builder()
                .clientId(cd.getId())
                .userEmail(cd.getUser().getEmail())
                .fullName(cd.getCustname() + " " + cd.getCustsurname())
                .firmName(cd.getFirmname())
                .pendingAddresses(new ArrayList<>())
                .pendingDocuments(new ArrayList<>())
                .build());
    }

    public List<Clientdetail> GetClienListWithNotApruvedAdresses() {
        List<Clientdetail> clientdetailList =  new ArrayList<>();
        List<Clientaddress> clientaddressList = GetNotApruvedAddresses();
        HashMap<Long, Integer> clientdetailLongHashMap = new HashMap<>();
        for (Clientaddress adr : clientaddressList) {
            if (!clientdetailLongHashMap.containsKey(adr.getClientdetail().getId())) {
                Clientdetail cd = adr.getClientdetail();
                clientdetailList.add(cd);
                clientdetailLongHashMap.put(cd.getId(), cd.getClientaddressList().size());
            }
        }
        return clientdetailList;
    }

    public List<Clientdetail> GetClientsWithNotApruvedDocoments() {
        List<Documents> documentsList = GetNotApruvedDocuments();
        List<Clientdetail> clientdetailList = new ArrayList<>();
        HashMap<Long, Integer> qDocuments = new HashMap<>();
        for (Documents doc : documentsList) {
            if (!qDocuments.containsKey(doc.getClientdetail().getId())) {
                clientdetailList.add(doc.getClientdetail());
                qDocuments.put(doc.getClientdetail().getId(), doc.getClientdetail().getDocumentslist().size());
            }
        }
        return clientdetailList;
    }

    public Users GetUsersById(Long iduser) {
        return clientService.GetUserById(iduser);
    }

    public List<Track> GetNotApruveTracks() {
        return createrService.GetNotApruveTracks();
    }

    public List<Track> GetAllTracks() {
        return createrService.GetAllTracks();
    }

    public Track GetTrackById(Long idTrack) {
        return createrService.GetTrackById(idTrack);
    }

    public void SaveTrack(Track track) {
        createrService.SaveTrack(track);
    }

    public void DeleteTrack(Long idTrack) {
        Track track = createrService.GetTrackById(idTrack);
        if (track != null) {
            if (track.getStoreitem() != null) {
                storeService.DeleteInStore(track.getStoreitem());
            }
            createrService.DeleteTrack(track);
        }
    }

    public void DeleteTrackByUuid(String trackUuid) {
        Track track = createrService.GetTrackByUuid(trackUuid);
        if (track != null) {
            if (track.getStoreitem() != null) {
                storeService.DeleteInStore(track.getStoreitem());
            }
            createrService.DeleteTrack(track);
        }
    }
}
