package media.toloka.rfa.radio.station.onlinelist.Service;

//import lombok.var;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.radio.station.ClientHomeStationController;
import media.toloka.rfa.radio.station.onlinelist.Model.ListOnlineFront;
import media.toloka.rfa.radio.station.onlinelist.Model.StationReport;
import media.toloka.rfa.radio.station.service.StationOnlineList;
import media.toloka.rfa.radio.station.service.StationService;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

//@Service
@Profile("Front")
@Component
public class ScheduledTasks {
//public class ListOnlineStationTimerTask extends TimerTask {

    @Autowired
    private StationService stationService;
    /*
    Клас, який періодично оновлює список онлайн станцій та ефірну інформацію (назва треку, автор)
    Ініціалізація відбувається при старті застосунку.
     */
//private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);
    final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);


    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Scheduled(fixedRate = 30000)
    public void GetStationOnlineList() {
        // будуємо перелік станцій, які зараз в онлайні для того, щоб не смикати через GET https при кожному зверненні
        // REST інтерфейc LibreTime

        // беремо станції, які знаходяться онлайн
        List<Station> stationOnlineList = stationService.GetListStationByStatus(true);
        // беремо перелік станцій для морди
        List<ListOnlineFront> listOnlineFronts = StationOnlineList.getInstance().GetOnlineList();

        Date currentUpdate = new Date();
        listOnlineFronts.clear();

        for (Station stationOnline : stationOnlineList) {
            // беремо зі станції поточний трек
            // todo передбачити оновлення елементів переліку замість очистки переліку станцій.
            // очищення списку станцій може вплинути на достовірність формування на морді. Наприклад,
            // перелік станцій буде взято в момент коли перелік ще пустий. Звісно, нічого трагічного
            // не станеться, але якось не охайно :)
            URL url;
            try {
                // сформували урл для станції
                url = URI.create("https://" + stationOnline.getDbname() + ".rfa.toloka.media/api/live-info/?callback").toURL();
            } catch (MalformedURLException e) {
                logger.info("GetStationOnlineList: Помилка формування URL.");
                continue;
            }
            String json;
            // Перекидаємо отриману строку в JSON
            try {
                json = IOUtils.toString(url, Charset.forName("UTF-8"));
            } catch (IOException e) {
                continue;
            }
// 1. Оголошуємо точний тип, який нам потрібен (Мапа String -> Object)
            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
// 2. Передаємо цей тип у Gson
            Gson gson = new GsonBuilder().create();
            // перевірили, чи є рядок коректним json рядком. Це на випадок, коли nginx віддасть html сторінку
            if (!isValidJson(gson,json)) continue;
            Map<String, Object> Report = gson.fromJson(json, mapType);
            Map<String, Object> current = (Map<String, Object>) Report.get("current");
            if (current != null) {
                Map<String, Object> currentmetadata = (Map<String, Object>) current.get("metadata");
                if (currentmetadata != null) {
                    String currentmetadataTrackTitle = (String) currentmetadata.get("track_title");
                    String currentmetadataArtistName = (String) currentmetadata.get("artist_name");
                    // Заповнюємо екземпляр станції
                    ListOnlineFront lof = new ListOnlineFront();
                    lof.setUuid(stationOnline.getUuid());
                    lof.setDbname(stationOnline.getDbname());
                    lof.setStationname(stationOnline.getName());
                    lof.setTrack(currentmetadataTrackTitle);
                    lof.setGroup(currentmetadataArtistName);
                    lof.setCurdate(currentUpdate);

                    listOnlineFronts.add(lof);
                }
            }
        }
        for (ListOnlineFront tlof : listOnlineFronts) {
            if (tlof.getCurdate() != currentUpdate) {
                // видаляємо застарілі елементи
                listOnlineFronts.remove(tlof);
            }
        }
    }

    public static boolean isValidJson(Gson gson, String jsonInString) {
        try {
            // Пробуємо розпарсити рядок у базовий внутрішній об'єкт Gson
            gson.fromJson(jsonInString, Object.class);
            return true;
        } catch (JsonSyntaxException e) {
            // Якщо синтаксис порушено, перехоплюємо виняток
            return false;
        }
    }


}
