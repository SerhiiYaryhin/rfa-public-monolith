package media.toloka.rfa.radio.repository;


import media.toloka.rfa.radio.model.Clientaddress;
import media.toloka.rfa.radio.model.Clientdetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientAddressRepository extends JpaRepository<Clientaddress, Long> {

    Clientaddress getReferenceById(Long id);

    List<Clientaddress> findByClientdetail(Clientdetail Clientdetail);

    List<Clientaddress> findByApruve(boolean b);


    //Id(Long id);

}
