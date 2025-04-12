package media.toloka.rfa.radio.stt.model;

import java.util.List;

public class WhisperResult {

    public class Root {
        public String text;
        public List<Segment> segments;
        public String language;
    }

    class Segment {
        public int id;
        public int seek;
        public double start;
        public double end;
        public String text;
        public Tokens tokens;
    }

    class Tokens {
        public int temperature;
        public double avg_logprob;
        public double compression_ratio;
        public double no_speech_prob;
    }

//    String json = /* JSON-рядок тут */;
//
//    Gson gson = new GsonBuilder().setPrettyPrinting().create();
//    Root root = gson.fromJson(json, Root.class);
}