package media.toloka.rfa.radio.creater.repository;

import media.toloka.rfa.radio.model.Album;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.Track;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface AlbumRepository extends JpaRepository<Album, Long> {

    List<Album> findByClientdetail(Clientdetail cd);
    List<Album> findByStoreuuidalbumcover(String storeuuidalbumcover);
    Album getReferenceById(Long id);
    Album findByUuid(String uuid);

    // 1. Отримати список усіх альбомів, які позначені як опубліковані (для сторінки /albums)
    List<Album> findByPublishedTrue();

    // Отримати опубліковані альбоми з пагінацією та сортуванням
    Page<Album> findByPublishedTrue(Pageable pageable);

    // 2. Отримати конкретний опублікований альбом разом з його треками за один SQL-запит.
    // Використання LEFT JOIN FETCH критично важливе, щоб уникнути помилки LazyInitializationException
    // при зверненні до album.getTrack() у Thymeleaf-шаблоні поза межами транзакції.
    @Query("SELECT a FROM Album a LEFT JOIN FETCH a.track WHERE a.uuid = :uuid AND a.published = true")
    Optional<Album> findByUuidAndPublishedTrue(@Param("uuid") String uuid);
}
