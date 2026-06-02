package media.toloka.rfa.radio.store.Service;

import media.toloka.rfa.author.model.AuthorArticle;
import media.toloka.rfa.author.model.AuthorColumn;
import media.toloka.rfa.author.repository.AuthorArticleRepository;
import media.toloka.rfa.author.repository.AuthorColumnRepository;
import media.toloka.rfa.podcast.model.PodcastChannel;
import media.toloka.rfa.podcast.model.PodcastItem;
import media.toloka.rfa.podcast.repositore.ChanelRepository;
import media.toloka.rfa.podcast.repositore.EpisodeRepository;
import media.toloka.rfa.radio.creater.repository.AlbumRepository;
import media.toloka.rfa.radio.creater.repository.TrackRepository;
import media.toloka.rfa.radio.model.Album;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Documents;
import media.toloka.rfa.radio.model.Post;
import media.toloka.rfa.radio.model.Track;
import media.toloka.rfa.radio.newstoradio.model.News;
import media.toloka.rfa.radio.newstoradio.repository.NewsRepositore;
import media.toloka.rfa.radio.post.repositore.PostRepositore;
import media.toloka.rfa.radio.repository.ClientDetailRepository;
import media.toloka.rfa.radio.repository.DocumentRepository;
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

    @Autowired
    private AuthorArticleRepository authorArticleRepository;

    @Autowired
    private AuthorColumnRepository authorColumnRepository;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ClientDetailRepository clientDetailRepository;

    @Autowired
    private NewsRepositore newsRepository;

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

        // 5. Авторські статті
        List<AuthorArticle> articles = authorArticleRepository.findByCoverUuid(store.getUuid());
        if (!articles.isEmpty()) dependencies.put("articles", articles);

        // 6. Авторські колонки
        List<AuthorColumn> columns = authorColumnRepository.findByAvatarUuid(store.getUuid());
        if (!columns.isEmpty()) dependencies.put("columns", columns);

        // 7. Альбоми
        List<Album> albums = albumRepository.findByStoreuuidalbumcover(store.getUuid());
        if (!albums.isEmpty()) dependencies.put("albums", albums);

        // 8. Документи
        List<Documents> docs = documentRepository.findByStore(store);
        if (docs.isEmpty()) {
            docs = documentRepository.findByStoreuuid(store.getUuid());
        }
        if (!docs.isEmpty()) dependencies.put("documents", docs);

        // 9. Фото профілю
        List<Clientdetail> profilePhotos = clientDetailRepository.findByProfilephoto(store);
        if (!profilePhotos.isEmpty()) dependencies.put("profilePhotos", profilePhotos);

        // 10. Новини
        List<News> news = newsRepository.findByStorespeach(store);
        if (!news.isEmpty()) dependencies.put("news", news);

        return dependencies;
    }
}
