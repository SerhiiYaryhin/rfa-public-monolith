package media.toloka.rfa.radio.creater.fileupload;

import media.toloka.rfa.radio.client.service.ClientService;
import media.toloka.rfa.radio.creater.service.CreaterService;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Track;
import media.toloka.rfa.radio.store.Service.StoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.InputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class CreaterDropPostFileControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ClientService clientService;

    @Mock
    private CreaterService createrService;

    @Mock
    private StoreService storeService;

    @InjectMocks
    private CreaterDropPostFileController controller;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void testUploadTrackReturnsUuid() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "test.mp3", "audio/mpeg", "test data".getBytes());
        Clientdetail cd = new Clientdetail();
        Track track = new Track();
        String testUuid = "test-track-uuid";
        track.setUuid(testUuid);

        when(clientService.GetClientDetailByUser(any())).thenReturn(cd);
        when(clientService.ClientCanDownloadFile(cd)).thenReturn(true);
        when(storeService.PutFileToStore(any(InputStream.class), eq("test.mp3"), eq(cd), any())).thenReturn("store-uuid");
        when(createrService.SaveTrackUploadInfo("store-uuid", cd)).thenReturn(track);

        // Act & Assert
        mockMvc.perform(multipart("/creater/trackupload").file(file))
                .andExpect(status().isOk())
                .andExpect(content().string(testUuid));
    }
}
