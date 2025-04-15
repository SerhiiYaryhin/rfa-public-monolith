package media.toloka.rfa.config.gson;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DateTimeGsonAdapters {

    // === java.util.Date ===
    public static class UtilDateAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {
        private final SimpleDateFormat formatter;

        public UtilDateAdapter() {
            formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        @Override
        public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(formatter.format(src));
        }

        @Override
        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            String value = sanitizeIsoString(json.getAsString());
            try {
                return formatter.parse(value);
            } catch (ParseException e) {
                throw new JsonParseException("Cannot parse java.util.Date: " + value, e);
            }
        }
    }

    // === LocalDateTime ===
    public static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        private final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.format(formatter));
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            String value = sanitizeIsoString(json.getAsString());
            return LocalDateTime.parse(value, formatter);
        }
    }

    // === ZonedDateTime ===
    public static class ZonedDateTimeAdapter implements JsonSerializer<ZonedDateTime>, JsonDeserializer<ZonedDateTime> {
        private final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

        @Override
        public JsonElement serialize(ZonedDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.format(formatter));
        }

        @Override
        public ZonedDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            String value = sanitizeIsoString(json.getAsString());
            return ZonedDateTime.parse(value, formatter);
        }
    }

    // === OffsetDateTime ===
    public static class OffsetDateTimeAdapter implements JsonSerializer<OffsetDateTime>, JsonDeserializer<OffsetDateTime> {
        private final DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        @Override
        public JsonElement serialize(OffsetDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.format(formatter));
        }

        @Override
        public OffsetDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            String value = sanitizeIsoString(json.getAsString());
            return OffsetDateTime.parse(value, formatter);
        }
    }

    // === Instant ===
    public static class InstantAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
        @Override
        public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString()); // ISO 8601
        }

        @Override
        public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return Instant.parse(json.getAsString());
        }
    }

    // === Utility: обрізає мікросекунди до мілісекунд ===
    private static String sanitizeIsoString(String iso) {
        if (iso.contains(".")) {
            int dotIndex = iso.indexOf(".");
            int end = dotIndex + 4;
            while (end < iso.length() && Character.isDigit(iso.charAt(end))) end++;
            String cleaned = iso.substring(0, Math.min(end, dotIndex + 4));
            if (iso.contains("Z")) cleaned += "Z"; // якщо було Z
            if (iso.contains("+")) cleaned += iso.substring(iso.indexOf("+")); // зона
            return cleaned;
        } else {
            return iso;
        }
    }
}

