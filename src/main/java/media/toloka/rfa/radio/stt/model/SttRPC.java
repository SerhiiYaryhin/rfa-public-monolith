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
    public class STTServer { // Сервер на який відправляємо повідомлення
        @Expose
        private String server = "null";
        @Expose
        private String user = "null";
    }

    @Data
    @ToString
    public class Front { //сервер з якого відправляємо повідомлення
        @Expose
        private String globalserver = null;
        @Expose
        private String localserver = null;
        @Expose
        private String user = null;
        @Expose
        private String addparametrs = null; // додаткові параметри
    }

    @Data
    @ToString
    public class sBackServer { // С
        @Expose
        private String addparametrs = "null";
    }

    @Expose
    private ERPCJobType rJobType = ERPCJobType.JOB_STT;
    @Expose
    private STTServer stt = new STTServer();
    @Expose
    private Front front = new Front();
    @Expose
    private sBackServer backServer = new sBackServer();
    @Expose
    private String sttUUID; // uuid запису на перетворення
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
