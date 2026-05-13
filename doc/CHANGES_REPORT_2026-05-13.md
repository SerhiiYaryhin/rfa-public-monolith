## Звіт про виконані роботи: Виправлення помилок RFA
Дата: 2026-05-13

## Огляд
Під час сесії було проведено комплексний аналіз та виправлення помилок, зазначених у RFA-currentError.txt. Головною причиною проблем була некоректна активація контролерів через профілювання та конфлікти в навігації.

## Деталі виправлень

### 1. Виправлення 404 помилок (Контролери)
- Виявлено, що анотації `@Profile("Front")` були закоментовані у багатьох контролерах (всього 45 файлів), що призводило до їх неактивності при запуску.
- Усі анотації `@Profile("Front")` були відновлені до робочого стану. Це забезпечило завантаження всіх ключових модулів:
  - Адміністрування, Креатор, Подкасти, Сховище.

### 2. Очищення навігації та роутингу
- Видалено дублюючий та нефункціональний пункт меню `/creater/podcasts/0` у `navbar.html`.
- Додано перевірку ролей для пункту "Сховище" у навігації, щоб запобігти 404/403 помилкам.

### 3. Виправлення відображення зображень
- У `StoreSiteController.java` додано обробку `null` для `contentMimeType` (fallback до `application/octet-stream`).

### 4. Рефакторинг завантаження подкастів (REST API)
- Створено новий контролер `PodcastFileLinkController` для уніфікації логіки прив'язки файлів до сутностей подкастів.
- Переведено `PodcastDropPostFileController` на використання REST-відповідей (`ResponseEntity`).
- Оновлено шаблони (`podcastcoverupload.html`, `podcastcoverepisodeupload.html`, `podcastepisodeupload.html`):
    - Налаштовано Dropzone на використання універсального ендпоінту `/api/store/upload`.
    - Реалізовано AJAX-виклик до `PodcastFileLinkController` після успішного завантаження файлу для прив'язки UUID до подкасту або епізоду.
    - Це дозволяє уникнути прямої залежності між завантаженням файлу та логікою створення сутностей у базі.

## Перелік модифікованих основних файлів
- `src/main/resources/templates/fragments/navbar.html`
- `src/main/java/media/toloka/rfa/radio/store/StoreSiteController.java`
- `src/main/java/media/toloka/rfa/podcast/fileupload/PodcastFileLinkController.java` (новий)
- `src/main/java/media/toloka/rfa/podcast/fileupload/PodcastDropPostFileController.java`
- `src/main/resources/templates/podcast/*.html` (оновлені JS-скрипти)
- (та контролери, у яких відновлено анотації `@Profile("Front")`)

