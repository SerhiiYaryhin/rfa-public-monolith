package media.toloka.rfa.podcast.service;

import media.toloka.rfa.podcast.model.PodcastChannel;
import media.toloka.rfa.podcast.model.PodcastItem;
import media.toloka.rfa.podcast.model.PodcastItunesCategory;
import media.toloka.rfa.podcast.repositore.ChanelRepository;
import media.toloka.rfa.podcast.repositore.EpisodeRepository;
import media.toloka.rfa.radio.store.Service.StoreService;
import media.toloka.rfa.radio.store.model.Store;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PodcastServiceTest {

    @Mock
    private ChanelRepository chanelRepository;

    @Mock
    private EpisodeRepository episodeRepository;

    @Mock
    private StoreService storeService;

    @InjectMocks
    private PodcastService podcastService;

    @Test
    public void testEqualsAndHashCodeAreSafeFromStackOverflow() {
        // Arrange bidirectional relationship
        PodcastChannel channel = new PodcastChannel();
        channel.setUuid("channel-uuid");
        channel.setTitle("Test Podcast");

        PodcastItem episode = new PodcastItem();
        episode.setUuid("episode-uuid");
        episode.setTitle("Test Episode");

        PodcastItunesCategory category = new PodcastItunesCategory();
        category.setUuid("category-uuid");

        // Link them bidirectionally
        channel.setItem(new ArrayList<>());
        channel.getItem().add(episode);
        episode.setChanel(channel);

        channel.setItunescategory(new ArrayList<>());
        channel.getItunescategory().add(category);
        category.setChanel(channel);

        // Act & Assert: This should NOT cause StackOverflowError
        assertDoesNotThrow(() -> {
            boolean equalsResult = episode.equals(new PodcastItem());
            int hash1 = episode.hashCode();
            int hash2 = channel.hashCode();
            int hash3 = category.hashCode();
            assertFalse(equalsResult);
        });
    }

    @Test
    public void testDeleteEpisodeRemovesFromChannelAndDeletes() {
        // Arrange
        PodcastChannel channel = new PodcastChannel();
        channel.setUuid("channel-uuid");

        PodcastItem episode = new PodcastItem();
        episode.setUuid("episode-uuid");
        episode.setChanel(channel);
        channel.setItem(new ArrayList<>());
        channel.getItem().add(episode);

        // Act
        podcastService.DeleteEpisode(episode, false);

        // Assert
        assertNull(episode.getChanel());
        assertTrue(channel.getItem().isEmpty());
        verify(chanelRepository, times(1)).save(channel);
        verify(episodeRepository, times(1)).delete(episode);
        verifyNoInteractions(storeService);
    }

    @Test
    public void testDeleteEpisodeAlsoDeletesPhysicalFile() {
        // Arrange
        PodcastChannel channel = new PodcastChannel();
        channel.setUuid("channel-uuid");

        PodcastItem episode = new PodcastItem();
        episode.setUuid("episode-uuid");
        episode.setChanel(channel);
        channel.setItem(new ArrayList<>());
        channel.getItem().add(episode);

        Store enclosure = new Store();
        enclosure.setUuid("file-store-uuid");
        episode.setEnclosurestore(enclosure);

        // Act
        podcastService.DeleteEpisode(episode, true);

        // Assert
        assertNull(episode.getEnclosurestore());
        verify(episodeRepository, atLeastOnce()).save(episode);
        verify(storeService, times(1)).DeleteInStore(enclosure);
        verify(chanelRepository, times(1)).save(channel);
        verify(episodeRepository, times(1)).delete(episode);
    }
}
