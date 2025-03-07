package media.toloka.rfa.radio.newstoradio.service;

import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.newstoradio.model.News;
import media.toloka.rfa.radio.newstoradio.repository.NewsRepositore;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.rpc.service.RPCSpeachService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Service
public class NewsService {

    @Autowired
    private NewsRepositore newsRepositore;

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

    public Long PutTxtToTmp(String sUuidNews, String nbody) {
        BufferedWriter writer = null;
        try
        {
            writer = new BufferedWriter( new FileWriter( "/tmp/"+sUuidNews+".tts"));
            writer.write( nbody);

        }
        catch ( IOException e)
        {
            logger.info("Помилка при запису tts файлу.");
            return 1L;
        }
        finally
        {
            try
            {
                if ( writer != null) writer.close( );
            }
            catch ( IOException e)
            {
                logger.info("Помилка при закритті tts файлу.");
                return 2L;
            }
        }
        return 0L;
    }

    public void deleteTmpFile(String sUuidNews) {
        String patch;
        patch = "/tmp/"+sUuidNews+".tts";
        File file = new File(patch);
        if (file.delete()) {
            logger.info("File deleted successfully:{}",patch);
        }
        else {
            logger.info("Failed to delete the file:",patch);
        }

        patch = "/tmp/"+sUuidNews+".wav";
        file = new File(patch);
        if (file.delete()) {
            logger.info("File deleted successfully:{}",patch);
        }
        else {
            logger.info("Failed to delete the file:{}",patch);
        }

        patch = "/tmp/"+sUuidNews+".mp3";
        file = new File(patch);
        if (file.delete()) {
            logger.info("File deleted successfully:{}",patch);
        }
        else {
            logger.info("Failed to delete the file:{}",patch);
        }
    }

    public Long RunTxtToMp3(String sUuidNews) {
        return 0L;
    }

    public Long PutMp3ToStore(String sUuidNews) {
        return 0L;
    }
}
