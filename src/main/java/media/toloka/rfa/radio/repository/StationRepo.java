package media.toloka.rfa.radio.repository;


import media.toloka.rfa.media.webrtc.model.dto.StationDto;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Contract;
import media.toloka.rfa.radio.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StationRepo extends JpaRepository<Station, Long> {
    List<Station> findStationByClientdetail(Clientdetail Clientdetailrfa);

//    Station save(Station station);

    Station getStationByDbname(String rstring);

    List<Station> findStationByContract(Contract contract);
    List<Station> findStationByClientdetailAndContract(Clientdetail clientdetail, Contract contract);

    List<Station> getStationByStationstate(boolean b);

    Station getStationByUuid(String uuid);

    List<Station> findByRoomuuidIsNotNull();

    /**
     * Отримуємо список станцій які мають кімнати з онлайн трансляцією.
     * @return
     */
    @Query("SELECT s FROM Station s " +
            "JOIN MessageRoom m ON s.roomuuid = m.uuid " +
            "WHERE s.roomuuid IS NOT NULL " +
            "AND m.roomOnlineStatus = true")
    List<Station> findStationsWithOnlineRooms();

//    @Query("SELECT new media.toloka.rfa.radio.dto.StationDto(" +
//            "s.id, s.name, s.description, s.uuid, s.roomuuid, s.stationstate, s.serverState, " +
//            "m.roomname, m.roomOnlineStatus, m.startonline) " +
//            "FROM Station s " +
//            "INNER JOIN MessageRoom m ON s.roomuuid = m.uuid " +
//            "WHERE s.roomuuid IS NOT NULL " +
//            "AND m.roomOnlineStatus = true")
//    List<StationDto> findActiveStationsDto();

    @Query("SELECT new media.toloka.rfa.media.messanger.webrtc.model.dto.StationDto(" +
            "  s.id, s.name, s.description, s.uuid, s.roomuuid, s.stationstate, s.serverState, " +
            "  m.roomname, m.roomOnlineStatus, m.startonline) " +
            "FROM Station s " +
            "INNER JOIN MessageRoom m ON s.roomuuid = m.uuid " +
            "WHERE s.roomuuid IS NOT NULL " +
            "  AND m.roomOnlineStatus = true")
    List<StationDto> findActiveStationsDto();
}


