package media.toloka.rfa.media.messenger.repository;


import media.toloka.rfa.media.messenger.model.MessageRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRoomRepository extends JpaRepository<MessageRoom, Long> {

    MessageRoom getById(Long id);
    MessageRoom getByUuid(String uuid);
    List<MessageRoom> findAll();
//    findByRoomuuidOrderBySendAsc

}
