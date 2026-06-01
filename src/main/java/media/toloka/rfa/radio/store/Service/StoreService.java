package media.toloka.rfa.radio.store.Service;


import media.toloka.rfa.radio.store.Reposirore.StoreRepositorePagination;
import media.toloka.rfa.radio.store.implementation.StoreFileImplementation;
import media.toloka.rfa.radio.dropfile.service.FilesService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.store.model.EStoreFileType;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.security.model.ERole;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

import static media.toloka.rfa.radio.store.model.EStoreFileType.*;

@Service
public class StoreService extends StoreFileImplementation {

    final Logger logger = LoggerFactory.getLogger(StoreService.class);

    @Autowired
    private StoreRepositorePagination storeRepositore;

    @Autowired
    private FilesService filesService;

    public boolean canUserAccessStoreItem(Store store, Users user) {
        if (store == null) return false;

        // Публічні типи контенту доступні всім
        EStoreFileType type = store.getStorefiletype();
        if (type == STORE_POSTCOVER ||
                type == STORE_PODCASTCOVER ||
                type == STORE_ALBUMCOVER ||
                type == STORE_BANNERIMAGE ||
                type == STORE_PHOTO) {
            return true;
        }

        // Для іншого контенту потрібна авторизація
        if (user == null) return false;

        // Власник завжди має доступ
        if (store.getClientdetail() != null &&
                store.getClientdetail().getUser() != null &&
                store.getClientdetail().getUser().getId().equals(user.getId())) {
            return true;
        }

        // Адмін-ролі мають доступ до всього
        for (media.toloka.rfa.security.model.Roles role : user.getRoles()) {
            ERole eRole = role.getRole();
            if (eRole == ERole.ROLE_ADMIN ||
                    eRole == ERole.ROLE_EDITOR ||
                    eRole == ERole.ROLE_MODERATOR) {
                return true;
            }
        }

        // Додаткова логіка: Треки та Епізоди доступні всім (але можна посилити перевіркою apruve у Track)
        if (type == STORE_TRACK || type == STORE_EPISODETRACK || type == STORE_TTS) {
            return true;
        }

        return false;
    }

    public List<Store> GetAllByClientId(Clientdetail cd) {
        return storeRepositore.findAllByClientdetail(cd);
    }

    public Page GetStorePage(int pageNumber, int pageCount) {
        Pageable storePage = PageRequest.of(pageNumber, pageCount, Sort.by("createdate").descending());
        Page page = storeRepositore.findAll(storePage);
        return page;
    }

    public Page GetStorePageByClientDetail(int pageNumber, int pageCount, Clientdetail cd) {
//        Pageable storePage = PageRequest.of(pageNumber, pageCount);
        return storeRepositore.findByClientdetailOrderByIdDesc(PageRequest.of(pageNumber, pageCount),cd);
//        return page;
    }

    public List<Store> GetAllTrackByClientId(Clientdetail cd) {
//        return storeRepositore.findByClientdetailAndStorefiletype(cd,STORE_TRACK);
        return storeRepositore.findByClientdetailAndStorefiletypeOrderByIdDesc(cd,STORE_TRACK);
    }

    public void SaveStore(Store store) {
        storeRepositore.save(store);
    }

    public List<Store> GetAllEpisodeByClientId(Clientdetail cd) {
        return storeRepositore.findByClientdetailAndStorefiletypeOrderByIdDesc(cd,STORE_EPISODETRACK);
    }

    public Page GetAllPictures(int pageNumber, int pageCount, Clientdetail cd) {
        Pageable storePage = PageRequest.of(pageNumber, pageCount);
        Page ttt = storeRepositore.findByStorelAllPictures(storePage, cd);
        return ttt; // storeRepositore.findByStorelAllPictures(storePage, cd);
//        return storeRepositore.findByStorelAllPictures(cd.getId());
    }

    public List<Store> GetPodcastCoverListByCd(Clientdetail cd) {
        return storeRepositore.findByClientdetailAndStorefiletype(cd, STORE_PODCASTCOVER);
    }

    public List<Store> GetPicturesListByClientDetail(Clientdetail cd) {
        Pageable allPicturesPage = PageRequest.of(0, 100); // Беремо останні 100 картинок
        return storeRepositore.findByStorelAllPictures(allPicturesPage, cd).getContent();
    }


    public Page<Store> GetPagingStoreFilesByType(int page, int size, EStoreFileType fileType) {
        PageRequest pageable = PageRequest.of(page, size);
        return storeRepositore.findByStorefiletype(pageable, fileType);
    }
}
