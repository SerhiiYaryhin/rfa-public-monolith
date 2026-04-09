package media.toloka.rfa.radio.station.service;

import media.toloka.rfa.radio.model.Station;
import media.toloka.rfa.radio.repository.StationRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StationServiceTest {

    @Mock
    private StationRepo stationRepo;

    @InjectMocks
    private StationService stationService;

    @Test
    void listAll_ShouldReturnAllStations() {
        // Arrange
        Station s1 = new Station();
        s1.setId(1L);
        Station s2 = new Station();
        s2.setId(2L);
        when(stationRepo.findAll()).thenReturn(Arrays.asList(s1, s2));

        // Act
        List<Station> result = stationService.listAll();

        // Assert
        assertEquals(2, result.size());
        verify(stationRepo, times(1)).findAll();
    }

    @Test
    void GetStationById_ShouldReturnStation_WhenExists() {
        // Arrange
        Station mockStation = new Station();
        mockStation.setId(5L);
        when(stationRepo.findById(5L)).thenReturn(Optional.of(mockStation));

        // Act
        Station result = stationService.GetStationById(5L);

        // Assert
        assertNotNull(result);
        assertEquals(5L, result.getId());
    }

    @Test
    void GetStationById_ShouldReturnNull_WhenNotExists() {
        // Arrange
        when(stationRepo.findById(10L)).thenReturn(Optional.empty());

        // Act
        Station result = stationService.GetStationById(10L);

        // Assert
        assertNull(result);
    }
}
