# Історія змін проекту RFA

## [2026-04-21] Виправлення помилки іменування властивостей JPA
- **Виправлено**: Помилка `PropertyReferenceException` при старті додатка. Назву методу `findByChanelOrderByPubdateDesc` у `EpisodeRepository` виправлено на `findByChanelOrderByPubDateDesc` (відповідно до назви поля `pubDate` у моделі `PodcastItem`).
- **Стабільність**: Усунуто проблему ініціалізації контексту Spring, що блокувала запуск сервера.

---

## [2026-04-21] Виправлення помилок компіляції в PodcastController
...
