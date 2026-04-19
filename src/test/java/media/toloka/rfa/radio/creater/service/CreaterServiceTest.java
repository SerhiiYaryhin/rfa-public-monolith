package media.toloka.rfa.radio.creater.service;

import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Track;
import media.toloka.rfa.radio.model.enumerate.EDocumentStatus;
import media.toloka.rfa.radio.creater.repository.TrackRepository;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreaterServiceTest {

    @Mock
    private TrackRepository trackRepository;

    @Mock
    private StoreService storeService;

    @Mock
    private SimpMessagingTemplate template;

    private CreaterService createrService;

    @BeforeEach
    public void setup() {
        createrService = new CreaterService(template);
        // Ручне інжектування полів, які не входять до конструктора
        ReflectionTestUtils.setField(createrService, "trackRepository", trackRepository);
        ReflectionTestUtils.setField(createrService, "storeService", storeService);
    }

    @Test
    public void testSaveTrackUploadInfoReturnsTrack() {
        // Arrange
        String storeUuid = "test-uuid";
        Clientdetail cd = new Clientdetail();
        Store store = new Store();
        store.setUuid(storeUuid);

        when(storeService.GetStoreByUUID(storeUuid)).thenReturn(store);
        when(trackRepository.save(any(Track.class))).thenAnswer(invocation -> {
            Track savedTrack = invocation.getArgument(0);
            savedTrack.setId(100L);
            return savedTrack;
        });

        // Act
        Track result = createrService.SaveTrackUploadInfo(storeUuid, cd);

        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(storeUuid, result.getStoreuuid());
        assertEquals(EDocumentStatus.STATUS_LOADED, result.getStatus());
        verify(trackRepository, times(1)).save(any(Track.class));
    }
}
