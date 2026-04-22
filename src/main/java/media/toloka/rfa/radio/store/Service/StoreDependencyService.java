package media.toloka.rfa.radio.store.Service;

import media.toloka.rfa.podcast.model.PodcastChannel;
import media.toloka.rfa.podcast.model.PodcastItem;
import media.toloka.rfa.podcast.repositore.ChanelRepository;
import media.toloka.rfa.podcast.repositore.EpisodeRepository;
import media.toloka.rfa.radio.creater.repository.TrackRepository;
import media.toloka.rfa.radio.model.Post;
import media.toloka.rfa.radio.model.Track;
import media.toloka.rfa.radio.post.repositore.PostRepositore;
import media.toloka.rfa.radio.store.model.Store;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StoreDependencyService {

    @Autowired
    private PostRepositore postRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private ChanelRepository chanelRepository;

    @Autowired
    private EpisodeRepository episodeRepository;

    public Map<String, List<?>> findDependencies(Store store) {
        Map<String, List<?>> dependencies = new HashMap<>();

        // 1. Пости (по UUID обкладинки)
        List<Post> posts = postRepository.findByCoverstoreuuid(store.getUuid());
        if (!posts.isEmpty()) dependencies.put("posts", posts);

        // 2. Треки
        List<Track> tracks = trackRepository.findByStoreitem(store);
        if (!tracks.isEmpty()) dependencies.put("tracks", tracks);

        // 3. Канали подкастів
        List<PodcastChannel> channels = chanelRepository.findByImagechanelstore(store);
        if (!channels.isEmpty()) dependencies.put("channels", channels);

        // 4. Епізоди (аудіо та картинки)
        List<PodcastItem> episodesAudio = episodeRepository.findByEnclosurestore(store);
        if (!episodesAudio.isEmpty()) dependencies.put("episodesAudio", episodesAudio);

        List<PodcastItem> episodesImage = episodeRepository.findByImagestoreitem(store);
        if (!episodesImage.isEmpty()) dependencies.put("episodesImage", episodesImage);

        return dependencies;
    }
}
