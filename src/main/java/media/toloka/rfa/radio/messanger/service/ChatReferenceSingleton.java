package media.toloka.rfa.radio.messanger.service;

import lombok.Data;
import media.toloka.rfa.radio.messanger.model.MessageRoom;
import org.springframework.beans.factory.annotation.Autowired;


//import org.hibernate.Query;


import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public final class ChatReferenceSingleton {

//    @Autowired
    private MessangerService messangerService = null;


    private static ChatReferenceSingleton instance = null;
    private  Map<String, String> roomMap = new HashMap<>();
    private  Map<String, String> usersMap = new HashMap<>();
    private  Map<String, Date> userlastlivetime = new HashMap<>();

    public static ChatReferenceSingleton getInstance() {
        if (instance == null) {
            instance = new ChatReferenceSingleton();
        }
        // fill roomMap
        return instance;
    }

    public Map<String, String> GetUsersMap() {
        return usersMap;
//        return this.usersMap;
    }
    public Map<String, String> GetRoomsMap() {
        return roomMap;
//        return this.roomMap;
    }

    public Map<String, Date> GetUserLastLiveTime() {
//        return this.userlastlivetime;
        return userlastlivetime;
    }

}
