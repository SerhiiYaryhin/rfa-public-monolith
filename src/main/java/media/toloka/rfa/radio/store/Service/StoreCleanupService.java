package media.toloka.rfa.radio.store.Service;

import jakarta.persistence.EntityNotFoundException;
import media.toloka.rfa.author.model.AuthorArticle;
import media.toloka.rfa.author.model.AuthorColumn;
import media.toloka.rfa.author.repository.AuthorArticleRepository;
import media.toloka.rfa.author.repository.AuthorColumnRepository;
import media.toloka.rfa.podcast.model.PodcastChannel;
import media.toloka.rfa.podcast.model.PodcastItem;
import media.toloka.rfa.podcast.repositore.ChanelRepository;
import media.toloka.rfa.podcast.repositore.EpisodeRepository;
import media.toloka.rfa.radio.creater.repository.TrackRepository;
import media.toloka.rfa.radio.creater.repository.AlbumRepository;
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
import media.toloka.rfa.security.model.ERole;
import media.toloka.rfa.security.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class StoreCleanupService {

    private final Logger logger = LoggerFactory.getLogger(StoreCleanupService.class);

    @Autowired
    private StoreService storeService;

    @Autowired
    private StoreDependencyService dependencyService;

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

    /**
     * Зануляє всі посилання на об'єкт Store у всіх сутностях системи.
     */
    @Transactional
    public void detachAll(Store store) {
        String uuid = store.getUuid();
        logger.info("Від'єднання об'єкта Store {} від усіх сутностей", uuid);

        // 1. Треки
        List<Track> tracks = trackRepository.findByStoreitem(store);
        for (Track t : tracks) {
            t.setStoreitem(null);
            trackRepository.save(t);
        }
        // Також за UUID, якщо використовується
        trackRepository.findAll().stream()
                .filter(t -> uuid.equals(t.getStoreuuid()))
                .forEach(t -> {
                    t.setStoreuuid(null);
                    trackRepository.save(t);
                });

        // 2. Пости
        List<Post> posts = postRepository.findByCoverstoreuuid(uuid);
        for (Post p : posts) {
            p.setCoverstoreuuid(null);
            postRepository.save(p);
        }

        // 3. Подкасти (Канали)
        List<PodcastChannel> channels = chanelRepository.findByImagechanelstore(store);
        for (PodcastChannel ch : channels) {
            ch.setImagechanelstore(null);
            chanelRepository.save(ch);
        }

        // 4. Подкасти (Епізоди)
        List<PodcastItem> episodesAudio = episodeRepository.findByEnclosurestore(store);
        for (PodcastItem ep : episodesAudio) {
            ep.setEnclosurestore(null);
            episodeRepository.save(ep);
        }
        List<PodcastItem> episodesImage = episodeRepository.findByImagestoreitem(store);
        for (PodcastItem ep : episodesImage) {
            ep.setImagestoreitem(null);
            episodeRepository.save(ep);
        }

        // 5. Авторські статті
        List<AuthorArticle> articles = authorArticleRepository.findByCoverUuid(uuid);
        for (AuthorArticle art : articles) {
            art.setCoverUuid(null);
            authorArticleRepository.save(art);
        }

        // 6. Авторські колонки
        List<AuthorColumn> columns = authorColumnRepository.findByAvatarUuid(uuid);
        for (AuthorColumn col : columns) {
            col.setAvatarUuid(null);
            authorColumnRepository.save(col);
        }

        // 7. Альбоми
        List<Album> albums = albumRepository.findByStoreuuidalbumcover(uuid);
        for (Album alb : albums) {
            alb.setStoreuuidalbumcover(null);
            albumRepository.save(alb);
        }

        // 8. Документи
        List<Documents> docs = documentRepository.findByStore(store);
        for (Documents d : docs) {
            d.setStore(null);
            documentRepository.save(d);
        }
        List<Documents> docsByUuid = documentRepository.findByStoreuuid(uuid);
        for (Documents d : docsByUuid) {
            d.setStoreuuid(null);
            documentRepository.save(d);
        }

        // 9. Фото профілю
        List<Clientdetail> profilePhotos = clientDetailRepository.findByProfilephoto(store);
        for (Clientdetail cd : profilePhotos) {
            cd.setProfilephoto(null);
            clientDetailRepository.save(cd);
        }

        // 10. Новини
        List<News> newsList = newsRepository.findByStorespeach(store);
        for (News n : newsList) {
            n.setStorespeach(null);
            newsRepository.save(n);
        }
    }

    /**
     * Перевіряє, чи може користувач видалити файл.
     */
    public boolean canUserDelete(Store store, Users user) {
        if (store == null || user == null) return false;

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getRole() == ERole.ROLE_ADMIN || r.getRole() == ERole.ROLE_EDITOR);
        
        if (isAdmin) return true;

        // Власник може видалити, якщо файл не в публічному контенті
        return store.getClientdetail() != null && 
               store.getClientdetail().getUser() != null && 
               store.getClientdetail().getUser().getId().equals(user.getId());
    }

    /**
     * Безпечне видалення об'єкта сховища.
     */
    @Transactional
    public void performSafeDelete(Store store, boolean onlyDetach) {
        detachAll(store);
        if (!onlyDetach) {
            storeService.DeleteInStore(store);
            logger.info("Об'єкт Store {} та його файл успішно видалено", store.getUuid());
        } else {
            logger.info("Об'єкт Store {} від'єднано, але залишено у сховищі", store.getUuid());
        }
    }

    /**
     * Безпечне видалення треку разом із його аудіофайлом.
     * Якщо файл використовується десь інде, він залишається.
     */
    @Transactional
    public void safeDeleteTrack(media.toloka.rfa.radio.model.Track track) {
        // 1. Отримуємо свіжий об'єкт у поточній транзакції (EntityManager),
        // щоб уникнути detached-стану та конфліктів версій
        media.toloka.rfa.radio.model.Track managedTrack = trackRepository.findById(track.getId())
                .orElseThrow(() -> new EntityNotFoundException("Трек не знайдено з ID: " + track.getId()));

        String trackUuid = managedTrack.getUuid();
        Store store = managedTrack.getStoreitem();

        // 2. Розірвання двосторонніх зв'язків у пам'яті (Це обов'язково для Hibernate/JPA!)
        if (managedTrack.getClientdetail() != null && managedTrack.getClientdetail().getTrackList() != null) {
            managedTrack.getClientdetail().getTrackList().remove(managedTrack);
        }

        if (managedTrack.getAlbum() != null && managedTrack.getAlbum().getTrack() != null) {
            managedTrack.getAlbum().getTrack().remove(managedTrack);
        }

        // Відв'язуємо Store від треку перед видаленням треку, щоб не було Foreign Key констрейнтів
        managedTrack.setStoreitem(null);

        // 3. Видаляємо ТРЕК стандартним методом репозиторію
        trackRepository.delete(managedTrack);

        // Примусово синхронізуємо зміни з БД прямо зараз всередині транзакції
        trackRepository.flush();

        // 4. Логіка очищення файлу (тепер вона повністю безпечна)
        if (store != null) {
            Map<String, List<?>> deps = dependencyService.findDependencies(store);
            if (deps.isEmpty()) {
                storeService.DeleteInStore(store);
                logger.info("Аудіофайл треку {} ({}) видалено (більше не використовується)", store.getFilename(), store.getUuid());
            } else {
                logger.info("Аудіофайл треку {} ({}) залишено, бо є інші залежності: {}",
                        store.getFilename(), store.getUuid(), deps.keySet());
            }
        }

        logger.info("Запис про трек {} успішно видалено з БД через JPA", trackUuid);
    }

    /**
     * Безпечне видалення альбому разом із його обкладинкою та розірванням зв'язків з треками.
     */
    @Transactional
    public void safeDeleteAlbum(Album album) {
        Album managedAlbum = albumRepository.findById(album.getId())
                .orElseThrow(() -> new EntityNotFoundException("Альбом не знайдено з ID: " + album.getId()));

        // Розірвання зв'язків з треками (встановлення album = null)
        if (managedAlbum.getTrack() != null) {
            for (Track track : managedAlbum.getTrack()) {
                track.setAlbum(null);
                trackRepository.save(track);
            }
            managedAlbum.getTrack().clear();
        }

        String coverUuid = managedAlbum.getStoreuuidalbumcover();

        // Видаляємо зв'язок з обкладинкою перед видаленням альбому
        managedAlbum.setStoreuuidalbumcover(null);
        albumRepository.delete(managedAlbum);
        albumRepository.flush();

        // Очищення файлу обкладинки зі сховища, якщо вона більше не використовується
        if (coverUuid != null) {
            Store coverStore = storeService.GetStoreByUUID(coverUuid);
            if (coverStore != null) {
                Map<String, List<?>> deps = dependencyService.findDependencies(coverStore);
                if (deps.isEmpty()) {
                    storeService.DeleteInStore(coverStore);
                    logger.info("Обкладинку альбому {} ({}) видалено (більше не використовується)", coverStore.getFilename(), coverStore.getUuid());
                } else {
                    logger.info("Обкладинку альбому {} ({}) залишено, бо є інші залежності: {}",
                            coverStore.getFilename(), coverStore.getUuid(), deps.keySet());
                }
            }
        }

        logger.info("Альбом {} успішно видалено з БД", album.getId());
    }
//    public void safeDeleteTrack(media.toloka.rfa.radio.model.Track track) {
//        Store store = track.getStoreitem();
//        String trackUuid = track.getUuid();
//
//        // СИНХРОНІЗАЦІЯ ДВОСТОРONНІХ ЗВ'ЯЗКІВ (Обов'язково для Hibernate)
//        // Відв'язуємо від клієнта
//        if (track.getClientdetail() != null && track.getClientdetail().getTrackList() != null) {
//            track.getClientdetail().getTrackList().remove(track);
//        }
//        // 2. Відв'язуємо від альбому, щоб не було конфліктів
//        track.setAlbum(null);
//
//        // 1. Видаляємо трек
//        track.setStoreitem(null);
//        track.setStoreuuid(null);
//        trackRepository.save(track);
//        trackRepository.delete(track);
//        // Забезпечуємо синхронізацію з БД перед пошуком залежностей
//        trackRepository.flush();
//        logger.info("Запис про трек {} видалено", trackUuid);
//
//        // 2. Якщо був файл, перевіряємо чи він ще десь потрібен
//        if (store != null) {
//            Map<String, List<?>> deps = dependencyService.findDependencies(store);
//            if (deps.isEmpty()) {
//                storeService.DeleteInStore(store);
//                logger.info("Аудіофайл треку {} ({}) видалено (більше не використовується)", store.getFilename(), store.getUuid());
//            } else {
//                logger.info("Аудіофайл треку {} ({}) залишено, бо він має інші залежності: {}",
//                        store.getFilename(), store.getUuid(), deps.keySet());
//            }
//        }
//    }
}
