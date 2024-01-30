package media.toloka.rfa.radio.repository;


import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StationRepo extends JpaRepository<Station, Long> {
    List<Station> findStationByClientdetail(Clientdetail Clientdetailrfa);

    Station save(Station station);

    Station getStationByDbname(String rstring);
}
