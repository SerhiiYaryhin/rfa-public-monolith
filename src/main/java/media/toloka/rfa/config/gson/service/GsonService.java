package media.toloka.rfa.config.gson.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import media.toloka.rfa.config.gson.*;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Date;

@Service
public class GsonService {
    public Gson CreateGson() {
        // https://www.javaguides.net/2019/11/gson-localdatetime-localdate.html

//  250415
//        GsonBuilder gbuilder = new GsonBuilder();
// спроба замінити адаптер для різних фломаттів дат на універсальний
//
//        gbuilder.registerTypeAdapter(LocalDate.class, new LocalDateSerializer());
//        gbuilder.registerTypeAdapter(LocalDate.class, new LocalDateDeserializer());
//        gbuilder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer());
//        gbuilder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer());
//        return gbuilder.setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();

        return  new GsonBuilder()
                .registerTypeAdapter(Date.class, new DateTimeGsonAdapters.UtilDateAdapter())
                .registerTypeAdapter(LocalDateTime.class, new DateTimeGsonAdapters.LocalDateTimeAdapter())
                .registerTypeAdapter(ZonedDateTime.class, new DateTimeGsonAdapters.ZonedDateTimeAdapter())
                .registerTypeAdapter(OffsetDateTime.class, new DateTimeGsonAdapters.OffsetDateTimeAdapter())
                .registerTypeAdapter(Instant.class, new DateTimeGsonAdapters.InstantAdapter())
                .excludeFieldsWithoutExposeAnnotation()
                .create();

    }
}
