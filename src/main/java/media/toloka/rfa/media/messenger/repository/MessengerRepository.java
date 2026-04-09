package media.toloka.rfa.media.messenger.repository;

import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Messages;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface MessengerRepository extends JpaRepository<Messages, Long> {


    List<Messages> findMessagesByFromOrTo(Clientdetail userFrom, Clientdetail UserTo);

    List<Messages> findMessagesByReadingAndTo(boolean reading, Clientdetail cd);

    List<Messages> findMessagesByFromOrToOrderBySendDesc(Clientdetail userFrom, Clientdetail UserTo);

    List<Messages> findMessagesByReadAndTo(LocalDateTime ldt, Clientdetail Clientdetailrfa);

}
