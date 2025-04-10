package media.toloka.rfa.radio.stt.service;

import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.newstoradio.model.News;
import media.toloka.rfa.radio.newstoradio.model.NewsRPC;
import media.toloka.rfa.radio.newstoradio.repository.NewsRepositore;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.Store;
import media.toloka.rfa.radio.stt.model.ESttStatus;
import media.toloka.rfa.radio.stt.model.Stt;
import media.toloka.rfa.radio.stt.model.SttRPC;
import media.toloka.rfa.radio.stt.repositore.SttRepositore;
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
public class STTBackServerService {

    @Autowired
    private SttRepositore sttRepositore;

    @Autowired
    private StoreService storeService;

    @Value("${media.toloka.tts.server.runGetFromSTT}")
    private String runGetFromStt;

    final Logger logger = LoggerFactory.getLogger(RPCSpeachService.class);


    public Page GetNewsPageByClientDetail(int pageNumber, int pageCount, Clientdetail cd) {
        Pageable NewsPage = PageRequest.of(pageNumber, pageCount);
        return sttRepositore.findByClientdetailOrderByCreatedateDesc(NewsPage, cd);
    }

    public Stt GetByUUID(String uuidstt) {
        return sttRepositore.getByUuid(uuidstt);
    }

    public void Save(Stt fnews) {
        sttRepositore.save(fnews);
    }

    public List<Stt> GetListNewsByCd(Clientdetail cd) {
        return sttRepositore.findByClientdetail(cd);
    }

    /// видаляємо трек зі сховища
    public Long deleteSttTrackFromStore(String sttuuid) {
        Stt stt = GetByUUID(sttuuid);
        Boolean storeRC = true;
        Store store = stt.getStorespeach();
        String storeUUID = "";
        if (store != null) {
            storeUUID = store.getUuid();
            if (stt.getStorespeach() != null) {

                stt.setStorespeach(null);
                storeRC =  storeService.DeleteStoreRecord(store);

                if (storeService.GetStoreByUUID(storeUUID) != null) storeRC = false;
                if (storeRC) {
                    logger.info("\nВидаляємо запис у сховищі. \n storeUUID={} \n sttUUID={}", storeUUID, sttuuid);
                    stt.setStorespeach(null);
                    Save(stt);
                }
            }
        }

        if (storeRC) {
            logger.info("\n===== Успішно видалили запис у сховищі. \nsttUUID={}  \nstoreUUID={}", sttuuid, storeUUID);
            return 0L;
        } else {
            return 1L;
        }
    }

    /// Видаляємо запис новини з бази
    public Long deleteStt(String uuidStt) {

        Long rc = deleteSttTrackFromStore(uuidStt);
        if (rc != 0L) return rc;
        sttRepositore.delete(GetByUUID(uuidStt));
        return rc;
    }

    /// Забираємо Файл з серверу, на якому відбувалося перетворення
    public Long GetResultFromStt(SttRPC rjob) {
        // Get FIles from tts server
        Long rc = 129L;
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", runGetFromStt);
        Map<String, String> env = pb.environment();
//        logger.info(rjob.getNewsUUID());
//        logger.info(rjob.getTts().getServer());
//        logger.info(rjob.getTts().getUser());
//        env.put("NEWSUUID", rjob.getNewsUUID());
//        env.put("TTSSERVER", rjob.getTts().getServer());
//        env.put("TTSUSER", rjob.getTts().getUser());

        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
//            logger.info("started");
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
//                logger.info(line);
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


        String patch = "/tmp/" + rjob.getSttUUID() + ".mp3";
        File initialFile = new File(patch);
        InputStream targetStream = null;
        try {
            targetStream = new FileInputStream(initialFile);
        } catch (FileNotFoundException e) {
            logger.info("==== Щось пішло не так! Не можу знайти результат TTS. {}", patch);
            return 100L;
        }
        if (GetByUUID(rjob.getSttUUID()) != null) {
            Clientdetail ccd = GetByUUID(rjob.getSttUUID()).getClientdetail();
            if (ccd != null) {
                String storeUUID = storeService.PutFileToStore(targetStream, rjob.getSttUUID() + ".mp3", GetByUUID(rjob.getSttUUID()).getClientdetail(), STORE_TTS);
                Stt stt = GetByUUID(rjob.getSttUUID());
                stt.setStorespeach(storeService.GetStoreByUUID(storeUUID));
                stt.setStatus(ESttStatus.STT_STATUS_DONE);
                Save(stt);
                logger.info("Stt uploaded file {}  StoreUUID {}", patch, storeUUID);
                return 0L;
            } else {
                logger.info("Stt uploaded file. Не можемо знайти Clientdetail новини за UUID {}", rjob.getSttUUID());
                return 1L;
            }
        } else {
            logger.info("Stt uploaded file. Не можемо знайти новину за UUID {}", rjob.getSttUUID());
            return 2L;
        }
    }
}
