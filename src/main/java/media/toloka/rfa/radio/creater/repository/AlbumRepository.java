package media.toloka.rfa.radio.creater.repository;

import media.toloka.rfa.radio.model.Album;
import media.toloka.rfa.radio.model.Clientdetail;
import media.toloka.rfa.radio.model.enumerate.EAlbumStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AlbumRepository extends JpaRepository<Album, Long> {

    List<Album> findByClientdetail(Clientdetail cd);
    List<Album> findByStoreuuidalbumcover(String storeuuidalbumcover);
    Album getReferenceById(Long id);
    Album findByUuid(String uuid);

    // ==========================================
    // Методи для ПУБЛІЧНОГО ДОСТУПУ (тільки схвалені + опубліковані)
    // ==========================================

    // Список опублікованих альбомів (тільки APPROVED)
    List<Album> findByPublishedTrueAndStatus(EAlbumStatus status);

    // Пагінований список опублікованих альбомів (тільки APPROVED)
    Page<Album> findByPublishedTrueAndStatus(EAlbumStatus status, Pageable pageable);

    // Останні 10 альбомів для головної сторінки
    List<Album> findTop10ByPublishedTrueAndStatusOrderByCreatedateDesc(EAlbumStatus status);

    // Конкретний альбом з треками (JOIN FETCH) для публічного перегляду
    @Query("SELECT a FROM Album a LEFT JOIN FETCH a.track WHERE a.uuid = :uuid AND a.published = true AND a.status = :status")
    Optional<Album> findByUuidAndPublishedTrueAndStatus(@Param("uuid") String uuid, @Param("status") EAlbumStatus status);

    // ==========================================
    // Методи для АДМІН-ПАНЕЛІ (бачать все)
    // ==========================================

    // Всі альбоми з пагінацією (для таблиці адміна)
    Page<Album> findAllByOrderByCreatedateDesc(Pageable pageable);

    // Лічильник для головної сторінки адмінки
    long countByStatus(EAlbumStatus status);
}