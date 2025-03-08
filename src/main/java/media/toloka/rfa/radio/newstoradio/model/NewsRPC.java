package media.toloka.rfa.radio.newstoradio.model;
/// Клас для передачі через Rabbitmq для перетворення тексту в голос
import com.google.gson.annotations.Expose;
import lombok.Data;
import lombok.ToString;
import media.toloka.rfa.rpc.model.ERPCJobType;

@Data
@ToString
public class NewsRPC {

    @Data
    @ToString
    public class TTS {
        @Expose
        private String server = null;
        @Expose
        private String user = null;
    }

    @Data
    @ToString
    public class Front {
        @Expose
        private String server = null;
        @Expose
        private String user = null;
    }
    @Expose
    private ERPCJobType rJobType;
    @Expose
    private TTS tts = new TTS();
    @Expose
    private Front front = new Front();
    @Expose
    private String newsUUID;
    @Expose
    private Long rc;
}
