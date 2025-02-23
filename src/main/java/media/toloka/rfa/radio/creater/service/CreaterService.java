package media.toloka.rfa.radio.creater.service;


import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.repository.AlbumCoverRepository;
import media.toloka.rfa.radio.creater.repository.AlbumRepository;
import media.toloka.rfa.radio.creater.repository.TrackRepository;
import media.toloka.rfa.radio.document.ClientDocumentEditController;
import media.toloka.rfa.radio.dropfile.service.FilesService;
import media.toloka.rfa.media.messanger.model.ChatMessage;
import media.toloka.rfa.media.messanger.model.enumerate.EChatRecordType;
import media.toloka.rfa.media.messanger.service.MessangerService;
import media.toloka.rfa.radio.login.service.TokenService;
import media.toloka.rfa.radio.model.*;
import media.toloka.rfa.radio.model.enumerate.EDocumentStatus;
import media.toloka.rfa.radio.post.repositore.PostRepositore;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CreaterService {

    final Logger logger = LoggerFactory.getLogger(ClientDocumentEditController.class);

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private AlbumCoverRepository albumCoverRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private PostRepositore postRepositore;

    @Autowired
    private FilesService filesService;

    @Autowired
    private MessangerService messangerService;

    @Autowired
    private StoreService storeService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private TokenService serviceToken;

    private SimpMessagingTemplate template;

    @Value("${media.toloka.rfa.server.chat.mainroom}")
    private String chatmainroom;
    @Value("${media.toloka.rfa.server.chat.trackroom}")
    private String chattrackroom;

    @Autowired
    public CreaterService(SimpMessagingTemplate template) {
        this.template = template;
    }

    public List<Album> GetAllAlbumsByCreater(Clientdetail cd) {
        List<Album> albumList = albumRepository.findByClientdetail(cd);
        return albumList;
    }

    public List<Albumсover> GetAlbumCoverByCd (Clientdetail cd) {
        List<Albumсover> albumсoverList = albumCoverRepository.findByClientdetail(cd);
        return albumсoverList;
    }

    public List<Store> storeListTrackByClientDetail(Clientdetail cd ) {
        return storeService.GetAllTrackByClientId(cd);
    }

    public Page GetTrackPageByClientDetail(int pageNumber, int pageCount, Clientdetail cd) {
        Pageable TrackPage = PageRequest.of(pageNumber, pageCount);
//        Page page = trackRepository.findByClientdetail(TrackPage,cd);
        Page page = trackRepository.findByClientdetailOrderByUploaddateDesc(TrackPage,cd);
        return page;

    }


    public List<Track> GetAllTracksByCreater(Clientdetail cd) {
        List<Track> trackList = trackRepository.findByClientdetail(cd);
        return trackList;
    }

    public List<Post> GetAllPostsByCreater(Clientdetail cd) {
        return postRepositore.findByClientdetailOrderByCreatedateDesc(cd);
    }

    public Page GetAllPostsByCreater(int pageNumber, int pageCount, Clientdetail cd) {
        Pageable storePage = PageRequest.of(pageNumber, pageCount);
        Page page = postRepositore.findByClientdetailOrderByCreatedateDesc(storePage,cd);
        return page;
    }

    public List<Post> GetAllPostsByApruveAndMusicPost(Boolean flag) {
        List<Post> postList = postRepositore.findByApruveOrderByCreatedateDesc( flag);
        return postList;
    }

    public void SaveTrackUploadInfo(String storeitemUUID, Clientdetail cd) {
        Track track = new Track();
        track.setStatus(EDocumentStatus.STATUS_LOADED);
        track.setClientdetail(cd);
        track.setStoreuuid(storeitemUUID);
        track.setTochat(false);
        track.setStoreitem(storeService.GetStoreByUUID(storeitemUUID));
        trackRepository.save(track);
    }

    public Track GetTrackById(Long idTrack) {
        return trackRepository.getById(idTrack);
    }

    public Track GetTrackByStoreuuid (String storeUuid) {
        return trackRepository.getByUuid(storeUuid);
    }

    public void SaveTrack(Track track) {
        trackRepository.save(track);
    }

    public Album GetAlbumById(Long id) {
        Album album = albumRepository.getById(id);
        return album;
    }

    public void SaveAlbum(Album album) {
        albumRepository.save(album);
    }

    public void SaveAlbumCoverUploadInfo(String storeUUID, Clientdetail cd ) {
        Albumсover albumсover = new Albumсover();
        albumсover.setClientdetail(cd);
        albumсover.setStoreuuid(storeUUID);
        albumсover.setStoreitem(storeService.GetStoreByUUID(storeUUID)); //
        albumCoverRepository.save(albumсover);
    }

    public List<Track> GetLastUploadTracks() {
        return trackRepository.findAllTop10ByOrderByUploaddateAsc();
    }

    public Page GetTrackPage(int pageNumber, int pageCount) {
        Pageable storePage = PageRequest.of(pageNumber, pageCount);
        Page page = trackRepository.findAllByOrderByUploaddateDesc(storePage);
        return page;
    }

    public Page GetPostPage(int pageNumber, int pageCount) {
        Pageable storePage = PageRequest.of(pageNumber, pageCount);
        Page page = postRepositore.findAllByOrderByPublishdateDesc(storePage);
        return page;
    }


    // todo переробити на роботу зі сховищем
    public Albumсover GetAlbumCoverById(Long alcoid) {
        return albumCoverRepository.getById(alcoid);
    }

    public void PublicTrackToChat(Track track, Clientdetail cd) {
        ChatMessage cm = new ChatMessage();
        cm.setFromname(cd.getCustname()+" "+cd.getCustsurname());
        cm.setFromuuid(cd.getUuid());
        cm.setBody(
                "<span style=\"color: red\"><b>Опубліковано новий трек! </b><br>"+"</span>"
                +track.getAutor()+"<br>"
                +track.getName()+"<br>"
                +"<audio id=\""+track.getStoreuuid() + "\" controls>"
                +"<source src=\"/store/audio/"
                +track.getStoreuuid()+"\"/>"
                +"</audio>"
        );
        cm.setRtype(EChatRecordType.RECORD_TYPE_MEDIA.ordinal());
        cm.setRoomuuid(chatmainroom);
        messangerService.SaveMessage(cm);
        try {
            this.template.convertAndSend("/topic/"+chatmainroom, cm);
        } catch (Exception e) {
            logger.info("PublicTrackToChat Exception");
            e.printStackTrace();
        }
        cm.setRoomuuid(chatmainroom);
        messangerService.SaveMessage(cm);
        try {
            this.template.convertAndSend("/topic/"+chatmainroom, cm);
        } catch (Exception e) {
            logger.info("PublicTrackToChat Exception");
            e.printStackTrace();
        }

        ChatMessage cmt = new ChatMessage();
        cmt.setFromuuid(cm.getFromuuid());
        cmt.setFromname(cm.getFromname());
        cmt.setRtype(cm.getRtype());
        cmt.setBody(cm.getBody());
        cmt.setRoomuuid(chattrackroom);
        messangerService.SaveMessage(cmt);
        try {
            this.template.convertAndSend("/topic/"+chattrackroom, cmt);
        } catch (Exception e) {
            logger.info("PublicTrackToChat Exception");
            e.printStackTrace();
        }

    }

    public Store GetStoreAlbumCoverByUUID(String storeUUID) {
        return storeService.GetStoreByUUID(storeUUID);

    }

    public Track GetTrackByUuid(String trackuuid) {
        return trackRepository.getByUuid(trackuuid);
    }

    public Post GetPostByUuid(String postUuid) {
        return postRepositore.getByUuid(postUuid);
    }


    public Store GetStoreByStoreuuid(String storeUUID) {
        return storeService.GetStoreByUUID(storeUUID);
    }

    public boolean checkTelegramTooken() {
        // Перевіряємо наявність Токена для привʼязки Телеграму в базі токенів для поточного Users
        Users user = clientService.GetCurrentUser();
        if (user == null) return false;
        Token token = serviceToken.findByUser(user);
        if (token == null) return false;

        return true;
    }

    public String getTelegramToken() {
        Users user = clientService.GetCurrentUser();
        if (user == null) return null;
//        return user.getClientdetail().getUuid();
        Token token = serviceToken.findByUser(user);
        if (token == null) return null;
        return token.getToken();
    }

    public Page GetPostPageByClientDetail(Integer pageNumber, int pageCount, Clientdetail cd) {
        Pageable storePage = PageRequest.of(pageNumber, pageCount);
        Page page = postRepositore.findByClientdetailOrderByCreatedateDesc(storePage,cd);
        return page;
    }
}
