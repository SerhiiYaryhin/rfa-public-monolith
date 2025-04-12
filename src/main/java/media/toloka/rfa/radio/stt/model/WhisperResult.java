package media.toloka.rfa.radio.stt.model;

import lombok.Data;

import java.util.List;

@Data
public class WhisperResult {

    @Data
    public class WhisperResultFull {
        public String text;
        public List<Segment> segments;
        public String language;
    }

    @Data
    public class Segment {
        public int id;
        public int seek;
        public double start;
        public double end;
        public String text;
        public List<Long> tokens;
        public int temperature;
        public double avg_logprob;
        public double compression_ratio;
        public double no_speech_prob;
//        public Tokens tokens;
    }

//    class Tokens {
//        public List<Integer> tokens; // Додано — список токенів
//
//    }

//    String json = /* JSON-рядок тут */;
//
//    Gson gson = new GsonBuilder().setPrettyPrinting().create();
//    Root root = gson.fromJson(json, Root.class);
}