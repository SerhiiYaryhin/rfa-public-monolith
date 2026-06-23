package media.toloka.rfa.media.messanger.webrtc.model.dto;

import com.google.gson.annotations.Expose;
import lombok.Data;
import media.toloka.rfa.radio.model.enumerate.EServerState;

import java.util.Date;

@Data
public class StationDto {

    @Expose
    private Long id;

    @Expose
    private String name; // Назва станції

    @Expose
    private String description; // Опис радіостанції

    @Expose
    private String uuid; // Бізнес-UUID станції

    @Expose
    private String roomuuid; // UUID кімнати для онлайн-мовлення

    @Expose
    private Boolean stationstate; // Чи запущена станція (isRunning)

    @Expose
    private EServerState serverState; // Стан сервера (активний, забанений тощо)

    // Додаткові поля кімнати, які ми підтягуємо з бази через SQL JOIN
    @Expose
    private String roomname; // Назва кімнати з чату

    @Expose
    private Boolean roomOnlineStatus; // Чи в онлайні сама кімната в месенджері

    @Expose
    private Date startonline; // Коли кімната перейшла в онлайн
}
