# Детальний план рефакторингу та виправлення підсистеми банерів

**Дата та час розробки плану:** 05.06.2026 23:26 (локальний час: 2026-06-05T23:26:21+03:00)  
**Статус:** Готовий до реалізації  

---

## Етап 1: Виправлення політик безпеки (Spring Security)

### Крок 1.1. Надання публічного доступу до API банерів для гостей
* **Ціль:** Дозволити неавторизованим користувачам переглядати банери та надсилати статистику переходів/переглядів.
* **Файли для зміни:** [SecurityConfig.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/security/config/SecurityConfig.java)
* **Зміна:** Додати шлях `/api/2.0/banners/**` до секції `.permitAll()`.
  ```java
  .requestMatchers(
      "/", "/home", "/register", "/saveUser", "/guest/**",
      "/post/**", "/podcast/**", "/rss/**", "/error/**", "/robots.txt", "/robot.txt",
      "/api/1.0/**", "/api/2.0/banners/**", ...
  ).permitAll()
  ```

### Крок 1.2. Обмеження адмін-панелі керування банерами
* **Ціль:** Обмежити загальний доступ до `/banners/**` (адміністрування) для звичайних гостей.
* **Файли для зміни:** [SecurityConfig.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/security/config/SecurityConfig.java)
* **Зміна:** Додати окреме правило доступу:
  ```java
  .requestMatchers("/banners/**").authenticated()
  ```

---

## Етап 2: Виправлення багів стрімінгу та медіа-файлів

### Крок 2.1. Додавання відео-ендпоінту для Range-стрімінгу
* **Ціль:** Створити ендпоінт `/store/video/{storeUUID}`, що підтримує перемотку відеофайлів.
* **Файли для зміни:** [StoreSiteController.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/radio/store/StoreSiteController.java)
* **Зміна:** Розширити анотацію `@GetMapping` методу `getStoreAudioToStream`, додавши підтримку мапінгу `/store/video/...`:
  ```java
  @GetMapping(value = {
      "/store/audio/{storeUUID}", "/store/audio/{storeUUID}/{fileName:.+}",
      "/store/video/{storeUUID}", "/store/video/{storeUUID}/{fileName:.+}"
  })
  public ResponseEntity<Resource> getStoreMediaToStream(...)
  ```
  Змінити логіку дефолтного MIME-типу, якщо його не знайдено в БД:
  ```java
  String mimeType = storeRecord.getContentMimeType();
  if (mimeType == null || mimeType.isEmpty()) {
      mimeType = storeRecord.getStorefiletype() == EStoreFileType.STORE_BANNERVIDEO ? "video/mp4" : "audio/mpeg";
  }
  ```

### Крок 2.2. Оновлення посилань у фронтенді та прев'ю
* **Ціль:** Замінити некоректний `/store/audio/...` на `/store/video/...` для відеофайлів.
* **Файли для зміни:** 
  - [banner.js](file:///home/coder/rfa/src/main/resources/static/js/banner/banner.js#L161)
  - [select-media-form.html](file:///home/coder/rfa/src/main/resources/templates/banner/select-media-form.html#L93)
* **Зміна:** Оновити теги `<source>` для відео, змінити префікс шляху з `/store/audio/` на `/store/video/`.

### Крок 2.3. Виправлення розмірів Large Rectangle
* **Ціль:** Задати коректні розміри стилю банера `336x280` замість дубльованих `300x250`.
* **Файли для зміни:** [EBannerStyle.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/banner/model/enumerate/EBannerStyle.java#L6)
* **Зміна:**
  ```java
  BANNER_TYPE_336_280px("Large Rectangle", 336, 280),
  ```

---

## Етап 3: Виправлення вразливостей та ізоляція даних клієнтів

### Крок 3.1. Впровадження перевірки володіння банером
* **Ціль:** Запобігти редагуванню/видаленню чужих банерів іншими рекламодавцями.
* **Файли для зміни:** [BannersController.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/banner/controller/BannersController.java)
* **Зміна:** Додати приватний метод валідації власності:
  ```java
  private boolean isBannerOwner(Banner banner, Clientdetail cd, Users user) {
      if (user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"))) {
          return true; // Адміністратор може редагувати все
      }
      return banner.getClientdetail() != null && cd != null && 
             banner.getClientdetail().getUuid().equals(cd.getUuid());
  }
  ```
  Впровадити виклик цього методу у методах `editForm`, `save`, `delete` та `clearBannerMedia`. У разі невідповідності повертати редірект на список банерів або помилку доступу.

### Крок 3.2. Обмеження самостійного схвалення банерів (Self-Approval)
* **Ціль:** Дозволити схвалення банерів тільки для ролей `ADMIN` та `MODERATOR`.
* **Файли для зміни:** [BannersController.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/banner/controller/BannersController.java#L171)
* **Зміна:** Додати анотацію безпеки `@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")` до методу `toggleApprove`. Також у методі `save` заборонити змінювати прапорець `approve`, якщо користувач не має прав модератора/адміна (брати значення зі старої версії з БД).

### Крок 3.3. Виправлення потенційного NullPointerException при завантаженні файлу
* **Ціль:** Запобігти збою системи при спробі завантаження файлу на неіснуючий UUID.
* **Файли для зміни:** [BannerDropPostFileController.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/banner/fileupload/BannerDropPostFileController.java)
* **Зміна:** Додати перевірку на `null`:
  ```java
  Banner banner = bannerService.BannerGetByUUID(buuid);
  if (banner == null) {
      logger.error("Спроба завантажити медіа для неіснуючого банера UUID: {}", buuid);
      return;
  }
  ```
  Також додати безпечний парсинг MIME-типу, перевіряючи наявність символу `/`:
  ```java
  String mime = store.getContentMimeType();
  String mediatype = "IMAGE"; // default fallback
  if (mime != null && mime.contains("/")) {
      mediatype = mime.substring(0, mime.indexOf('/')).toUpperCase();
  }
  ```

---

## Етап 4: Оптимізація продуктивності (Performance Tuning)

### Крок 4.1. Виправлення дублювання запитів пагінації
* **Ціль:** Усунути подвійне завантаження сторінки банерів з БД.
* **Файли для зміни:** [BannersController.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/banner/controller/BannersController.java#L60-L64)
* **Зміна:** Отримувати контент безпосередньо зі змінної `bannersPage`:
  ```java
  Page<Banner> bannersPage = bannerService.BannerGetPage(page, size);
  model.addAttribute("bannersPage", bannersPage);
  model.addAttribute("banners", bannersPage.getContent());
  ```

### Крок 4.2. Фільтрація банерів на рівні бази даних
* **Ціль:** Уникнути `findAll()` з наступною фільтрацією в пам'яті через Java Stream у REST API.
* **Файли для зміни:**
  - `BannerRepository.java`
  - [BannerRestController.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/banner/controllerREST/BannerRestController.java)
* **Зміна:** Додати в репозиторій методи:
  ```java
  List<Banner> findByApproveTrue();
  List<Banner> findByApproveTrueAndBannertype(EBannerType bannertype);
  ```
  Та використовувати їх у `getApprovedBanners(...)` REST-контролера.
