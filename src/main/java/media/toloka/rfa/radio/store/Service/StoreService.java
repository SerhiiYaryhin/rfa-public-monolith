package media.toloka.rfa.radio.store.Service;


import media.toloka.rfa.radio.store.repository.StoreRepositoryPagination;
import media.toloka.rfa.radio.store.implementation.StoreFileImplementation;
import media.toloka.rfa.radio.dropfile.service.FilesService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.store.model.EStoreFileType;
import media.toloka.rfa.radio.store.model.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static media.toloka.rfa.radio.store.model.EStoreFileType.*;

@Service
@Transactional(readOnly = true)
public class StoreService extends StoreFileImplementation {

    final Logger logger = LoggerFactory.getLogger(StoreService.class);

    @Autowired
    private StoreRepositoryPagination storeRepositoryPagination;

    @Autowired
    private FilesService filesService;

    public List<Store> GetAllByClientId(Clientdetail cd) {
        return storeRepositoryPagination.findAllByClientdetail(cd);
    }

    public Page GetStorePage(int pageNumber, int pageCount) {
        Pageable storePage = PageRequest.of(pageNumber, pageCount);
        Page page = storeRepositoryPagination.findAll(storePage);
        return page;
    }

    public Page GetStorePageByClientDetail(int pageNumber, int pageCount, Clientdetail cd) {
//        Pageable storePage = PageRequest.of(pageNumber, pageCount);
        return storeRepositoryPagination.findByClientdetailOrderByIdDesc(PageRequest.of(pageNumber, pageCount),cd);
//        return page;
    }

    public List<Store> GetAllTrackByClientId(Clientdetail cd) {
//        return storeRepositoryPagination.findByClientdetailAndStorefiletype(cd,STORE_TRACK);
        return storeRepositoryPagination.findByClientdetailAndStorefiletypeOrderByIdDesc(cd,STORE_TRACK);
    }

    @Transactional
    public void SaveStore(Store store) {
        storeRepositoryPagination.save(store);
    }

    public List<Store> GetAllEpisodeByClientId(Clientdetail cd) {
        return storeRepositoryPagination.findByClientdetailAndStorefiletypeOrderByIdDesc(cd,STORE_EPISODETRACK);
    }

    public Page GetAllPictures(int pageNumber, int pageCount, Clientdetail cd) {
        Pageable storePage = PageRequest.of(pageNumber, pageCount);
        Page ttt = storeRepositoryPagination.findByStorelAllPictures(storePage, cd);
        return ttt; // storeRepositoryPagination.findByStorelAllPictures(storePage, cd);
//        return storeRepositoryPagination.findByStorelAllPictures(cd.getId());
    }

    public List<Store> GetPodcastCoverListByCd(Clientdetail cd) {
        return storeRepositoryPagination.findByClientdetailAndStorefiletype(cd, STORE_PODCASTCOVER);
    }


    public Page<Store> GetPagingStoreFilesByType(int page, int size, EStoreFileType fileType) {
        PageRequest pageable = PageRequest.of(page, size);
        return storeRepositoryPagination.findByStorefiletype(pageable, fileType);
    }
}
