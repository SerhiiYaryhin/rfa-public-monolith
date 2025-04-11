package media.toloka.rfa.radio.stt.model;
/// Клас для передачі через Rabbitmq для перетворення  голосу в текст
import com.google.gson.annotations.Expose;
import lombok.Data;
import lombok.ToString;
import media.toloka.rfa.rpc.model.ERPCJobType;

@Data
@ToString
public class SttRPC {

    @Data
    @ToString
    public class STTServer {
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

    @Data
    @ToString
    public class sBackServer {
        @Expose
        private String server = null;
    }

    @Expose
    private ERPCJobType rJobType = ERPCJobType.JOB_STT;
    @Expose
    private STTServer server= new STTServer();
    @Expose
    private Front front = new Front();
    @Expose
    private sBackServer backServer;
    @Expose
    private String sttUUID;
    @Expose
    private String text;
    @Expose
    private String uuidvoice;
    @Expose
    private String filenamevoice;
    @Expose
    private String model;
    @Expose
    private Long rc;
}
