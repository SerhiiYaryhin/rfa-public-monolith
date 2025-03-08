package media.toloka.rfa.radio.newstoradio.service;

import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.newstoradio.model.News;
import media.toloka.rfa.radio.newstoradio.model.NewsRPC;
import media.toloka.rfa.radio.newstoradio.repository.NewsRepositore;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.rpc.service.RPCSpeachService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;

import static media.toloka.rfa.radio.newstoradio.model.ENewsStatus.NEWS_STATUS_READY;
import static media.toloka.rfa.radio.store.model.EStoreFileType.STORE_TTS;

@Service
public class NewsService {

    @Autowired
    private NewsRepositore newsRepositore;

    @Autowired
    private StoreService storeService;

    final Logger logger = LoggerFactory.getLogger(RPCSpeachService.class);


    public Page GetNewsPageByClientDetail(int pageNumber, int pageCount, Clientdetail cd) {
        Pageable NewsPage = PageRequest.of(pageNumber, pageCount);
        return newsRepositore.findByClientdetailOrderByCreatedateDesc(NewsPage,cd);
    }

    public News GetByUUID(String uuidnews) {
        return newsRepositore.getByUuid(uuidnews);
    }

    public void Save(News fnews) {
        newsRepositore.save(fnews);
    }

    public List<News> GetListNewsByCd(Clientdetail cd) {
        return newsRepositore.findByClientdetail(cd);
    }

    public Long PutMp3FromTmpToStore(String sUuidNews) {
        // Move file from TTS server

        String patch = "/tmp/" + sUuidNews + ".mp3";
        File initialFile = new File(patch);
        InputStream targetStream = null;
        try {
            targetStream = new FileInputStream(initialFile);
        } catch (FileNotFoundException e) {
            logger.info("==== Щось пішло не так! Не можу знайти результат TTS. {}", patch);
            return 100L;
        }
        String storeUUID = storeService.PutFileToStore(targetStream, sUuidNews + ".mp3", GetByUUID(sUuidNews).getClientdetail(), STORE_TTS);
        News news = GetByUUID(sUuidNews);
        news.setStorespeach(storeService.GetStoreByUUID(storeUUID));
        news.setStatus(NEWS_STATUS_READY);
        Save(news);
        logger.info("News uploaded file {}  StoreUUID {}", patch,storeUUID);

        return 0L;
    }
    public Long GetMp3FromTTS(NewsRPC rjob) {
        // Забираємо файли з сервера TTS після перетворення
        return 0L;
    }
}
