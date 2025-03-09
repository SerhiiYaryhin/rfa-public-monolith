package media.toloka.rfa.radio.newstoradio.service;

import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.newstoradio.model.News;
import media.toloka.rfa.radio.newstoradio.model.NewsRPC;
import media.toloka.rfa.radio.newstoradio.repository.NewsRepositore;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.rpc.service.RPCSpeachService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;
import java.util.Map;

import static media.toloka.rfa.radio.newstoradio.model.ENewsStatus.NEWS_STATUS_READY;
import static media.toloka.rfa.radio.store.model.EStoreFileType.STORE_TTS;

@Service
public class NewsService {

    @Autowired
    private NewsRepositore newsRepositore;

    @Autowired
    private StoreService storeService;

    @Value("${media.toloka.tts.server.runGetFromTTS}")
    private String runGetFromTTS;

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

    public Long deleteNewsFromStore(String uuidNews) {
        News news = GetByUUID(uuidNews);
        Boolean storeRC = storeService.DeleteInStore(news.getStorespeach());
        if (storeRC) {
            Save(news);
            return 0L;
        }
        else
            return 1L;
    }

    public Long deleteNews(String uuidNews) {
        Long rc = deleteNewsFromStore(uuidNews);
        if ( rc != 0L) return rc;
        newsRepositore.delete(GetByUUID(uuidNews));
        return rc;
    }

    public Long GetMp3FromTts(NewsRPC rjob) {
        // Get FIles from tts server
        Long rc = 129L;
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", runGetFromTTS);
        Map<String, String> env = pb.environment();
        logger.info(rjob.getNewsUUID());
        logger.info(rjob.getTts().getServer());
        logger.info(rjob.getTts().getUser());
        env.put("NEWSUUID", rjob.getNewsUUID());
        env.put("TTSSERVER", rjob.getTts().getServer());
        env.put("TTSUSER", rjob.getTts().getUser());

        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info(line);
            }
            int exitcode = p.waitFor();
            rc = Long.valueOf(exitcode);
        } catch (IOException e) {
            logger.warn(" Щось пішло не так при виконанні завдання в операційній системі");
            e.printStackTrace();
        } catch (InterruptedException e) {
            logger.warn(" Щось пішло не так при виконанні завдання (p.waitFor) InterruptedException");
            e.printStackTrace();
        }


        String patch = "/tmp/" + rjob.getNewsUUID() + ".mp3";
        File initialFile = new File(patch);
        InputStream targetStream = null;
        try {
            targetStream = new FileInputStream(initialFile);
        } catch (FileNotFoundException e) {
            logger.info("==== Щось пішло не так! Не можу знайти результат TTS. {}", patch);
            return 100L;
        }
        String storeUUID = storeService.PutFileToStore(targetStream, rjob.getNewsUUID() + ".mp3", GetByUUID(rjob.getNewsUUID()).getClientdetail(), STORE_TTS);
        News news = GetByUUID(rjob.getNewsUUID());
        news.setStorespeach(storeService.GetStoreByUUID(storeUUID));
        news.setStatus(NEWS_STATUS_READY);
        Save(news);
        logger.info("News uploaded file {}  StoreUUID {}", patch,storeUUID);

        return 0L;
    }
}
