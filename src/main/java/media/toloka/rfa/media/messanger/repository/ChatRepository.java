package media.toloka.rfa.media.messanger.repository;

import media.toloka.rfa.media.messanger.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatRepository extends JpaRepository<ChatMessage, Long> {

    ChatMessage getReferenceById(Long id);
    ChatMessage getByUuid(String uuid);

//    @Query(
//            value = "SELECT * FROM ChatMessage u WHERE u.roomuuid = null",
//            nativeQuery = true)
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.roomuuid IS NULL and (cm.fromuuid = ?1 or cm.touuid = ?1) ORDER BY send ASC LIMIT 1000")
    List<ChatMessage> findByPrivateMessageByUserUuid(String useruuid);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.roomuuid = ?1 ORDER BY send ASC LIMIT 1000")
    List<ChatMessage> findByRoomuuidOrderBySendAsc(String roomuuid);

}
