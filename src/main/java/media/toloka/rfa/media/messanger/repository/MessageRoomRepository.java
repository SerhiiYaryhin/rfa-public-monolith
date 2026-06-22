package media.toloka.rfa.media.messanger.repository;


import media.toloka.rfa.media.messanger.model.MessageRoom;
import media.toloka.rfa.media.messanger.model.enumerate.EChatRoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRoomRepository extends JpaRepository<MessageRoom, Long> {

    MessageRoom getReferenceById(Long id);
    MessageRoom getByUuid(String uuid);
    List<MessageRoom> findAll();
    List<MessageRoom> getByRoomtype(EChatRoomType roomType);
//    findByRoomuuidOrderBySendAsc

}
