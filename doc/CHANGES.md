# Історія змін проекту RFA

## [2026-04-21] Виправлення помилки відображення дати епізодів
- **Виправлено**: Помилка `TemplateProcessingException` у `podcast/view.html`. Назву поля `item.pubdate` змінено на коректну `item.pubDate`, що дозволило Thymeleaf правильно відображати дату публікації епізодів.

---

## [2026-04-21] Виправлення помилки іменування властивостей JPA
- **Виправлено**: Помилка `PropertyReferenceException` при старті додатка. Назву методу `findByChanelOrderByPubdateDesc` у `EpisodeRepository` виправлено на `findByChanelOrderByPubDateDesc`.
- **Стабільність**: Усунуто проблему ініціалізації контексту Spring, що блокувала запуск сервера.

---

## [2026-04-21] Виправлення помилок компіляції в PodcastController
...
