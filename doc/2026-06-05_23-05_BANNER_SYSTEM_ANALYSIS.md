# Аналіз підсистеми роботи з банерами

**Дата та час аналізу:** 05.06.2026 23:05 (локальний час: 2026-06-05T23:05:05+03:00)  
**Об'єкт аналізу:** Модуль керування та відображення рекламних банерів (текстових, зображень, аудіо та відео).

---

## 1. Загальний опис підсистеми
Підсистема банерів призначена для створення, редагування, схвалення, відображення та аналізу ефективності (перегляди й переходи) рекламних банерів. Вона складається з:
- **Бази даних та моделей:** Entity-клас `Banner` та допоміжні переліки (Enums).
- **Сховища та сервісу:** `BannerRepository` та `BannerService`.
- **Контролерів:** 
  - MVC-контролер (`BannersController`) для адміністративного інтерфейсу.
  - REST-контролери (`BannerRestController` та `BannerDropPostFileController`) для завантаження медіафайлів та інтеграції з клієнтським JavaScript.
- **Фронтенду:** Скрипт `banner.js` для динамічного завантаження банерів у розмітку та реєстрації статистики, а також Thymeleaf-шаблони.

---

## 2. Детальний аналіз компонентів та виявлені проблеми

### 2.1. Рівень даних (Model & Repository)

#### [Banner.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/banner/model/Banner.java)
* **Проблема генерації ID (`@PrePersist`):**
  Метод `generateUUID()` використовує `System.currentTimeMillis()` для ініціалізації `id` (типу `Long`):
  ```java
  @PrePersist
  public void generateUUID() {
      if (this.id == null) {
          this.id = System.currentTimeMillis();
      }
  }
  ```
  *Критичність:* **Середня**. Використання системного часу для первинного ключа в багатокористувацьких системах призводить до колізій (ID collision), якщо два записи зберігаються одночасно протягом однієї мілісекунди. Слід використовувати автоінкрементну стратегію бази даних (`GenerationType.IDENTITY`) або UUID.
* **Помилка у назві пакету репозиторію:**
  Пакет визначено як `media.toloka.rfa.banner.repositore` замість стандартного `repository`.
  *Критичність:* **Низька** (проблема чистоти коду).

#### [EBannerStyle.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/banner/model/enumerate/EBannerStyle.java)
* **Помилка у визначенні розмірів (Bug):**
  Елемент `BANNER_TYPE_336_280px` містить некоректні розміри по горизонталі (`h`) та вертикалі (`v`):
  ```java
  BANNER_TYPE_336_280px("Large Rectangle", 300, 250),
  ```
  Він дублює значення стилю `BANNER_TYPE_300_250px`.
  *Критичність:* **Середня**. Призводить до викривлення пропорцій або неправильної верстки на фронтенді, якщо розміри беруться безпосередньо з констант стилю. Має бути `336, 280`.

---

### 2.2. Рівень бізнес-логіки (Service & Repository)

#### [BannerRepository.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/banner/repositore/BannerRepository.java)
* Містить закоментовані методи пошуку банерів за клієнтом та статусом схвалення.
* Перевизначено метод `void deleteById(String uuid);`, хоча він уже успадковується від `JpaRepository<Banner, String>`.

#### [BannerService.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/banner/service/BannerService.java)
* Надає стандартні CRUD методи (`BannerGetPage`, `BannerGetAll`, `BannerGetByUUID`, `BannerSave`, `BannerDelete`).
* Логіка проста, додаткових перевірок безпеки чи фільтрації не містить.

---

### 2.3. Контролери та REST API

#### [BannersController.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/banner/controller/BannersController.java) (MVC)
* **Дублювання запитів до бази даних (Performance Bug):**
  У методі `list(...)` сервіс викликається двічі для одного й того самого набору даних:
  ```java
  Page<Banner> bannersPage = bannerService.BannerGetPage(page, size);
  model.addAttribute("bannersPage", bannersPage);
  model.addAttribute("banners", bannerService.BannerGetPage(page, size));
  ```
  *Критичність:* **Низька/Середня** (впливає на продуктивність бази даних). Достатньо зробити запит один раз і передавати `bannersPage.getContent()` у модель як `"banners"`.
* **Копіювання властивостей при збереженні:**
  У методі `save(...)`, якщо банер вже існує в БД, властивості копіюються вручну:
  ```java
  cur_banner.setTitle(banner.getTitle());
  ...
  cur_banner.setStore(banner.getStore()); // це поле НЕ копіюється!
  ```
  Поле `store` не копіюється з переданого об'єкта, що може призвести до втрати зв'язку з сутністю медіафайлу сховища, якщо оновлюється текстовий вміст банера.

#### [BannerDropPostFileController.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/banner/fileupload/BannerDropPostFileController.java) (File Upload REST)
* **Небезпека виникнення NullPointerException (Crash Bug):**
  У методі `BannerMediaUpload(...)` на лінії 69 отримується банер за його UUID:
  ```java
  Banner banner = bannerService.BannerGetByUUID(buuid);
  ```
  Якщо UUID некоректний або банер не знайдено, змінна `banner` буде дорівнювати `null`. Проте на лінії 76 виконується звернення без перевірки:
  ```java
  banner.setUuidmedia(storeUUID); // Викличе NullPointerException
  ```
  *Критичність:* **Середня** (помилка стабільності системи).
* **Небезпека при розборі MIME-типу (MIME Parsing Bug):**
  На лінії 78:
  ```java
  String mediatype = store.getContentMimeType().substring(0, store.getContentMimeType().indexOf('/')).toUpperCase();
  ```
  Якщо `contentMimeType` є `null` або не містить символу `/`, виникне виняток `NullPointerException` чи `StringIndexOutOfBoundsException`. Слід застосувати безпечніші методи перевірки типу файлу.
* **Відсутність перевірки володіння банером (Security Issue):**
  Метод перевіряє лише те, чи має користувач загальне право завантажувати файли (`clientService.ClientCanDownloadFile(cd)`), але не перевіряє, чи належить банер `buuid` саме цьому клієнту. Користувач-клієнт "А" може завантажити медіафайл для банера клієнта "Б".
  *Критичність:* **Висока** (порушення ізоляції даних).

#### [BannerRestController.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/banner/controllerREST/BannerRestController.java) (REST API)
* **Слабке місце у безпеці:**
  Аналогічно до завантаження файлу, у методі `updateBannerMedia` перевіряється авторизація клієнта, але немає верифікації володіння банером. Кожен авторизований клієнт може викликати `PUT /api/2.0/banners/{bannerUuid}/media/{mediaUuid}` і підмінити медіафайл для будь-якого банера в системі.
* **Метод `getApprovedBanners` / `load`:**
  Замість використання SQL-запитів з фільтрацією на рівні бази даних, ці методи вигрібають весь список банерів `bannerRepository.findAll()` та фільтрують їх засобами Java Stream API. Це критично знижує продуктивність за великої кількості банерів.

---

### 2.4. Інтеграція та фронтенд

#### [banner.js](file:///home/coder/rfa/src/main/resources/static/js/banner/banner.js)
* **Використання аудіо-ендпоінту для стрімінгу відео:**
  На лінії 161 (секція `case 'VIDEO'`):
  ```javascript
  <video controls class="w-100">
      <source src="/store/audio/${banner.uuidmedia}" >
      Ваш браузер не підтримує відео-елемент.
  </video>
  ```
  *Опис:* Використання `/store/audio/...` є виправданим технічним рішенням, оскільки цей ендпоінт у `StoreSiteController` реалізує стрімінг файлів з підтримкою HTTP Range (дозволяє клієнту виконувати перемотку та позиціонування медіафайлів).
  *Рекомендація:* Для дотримання чистоти архітектури та коректного підвантаження типів контенту (уникнення дефолтного `audio/mpeg` для відео), раціонально додати окремий додатковий мапінг для відео (наприклад, `/store/video/{storeUUID}`) до того самого методу стрімінгу контролера, замість використання `/store/audio/` для відеофайлів.
* **Відсутність обробки помилок при кліках:**
  У обробнику кліків `track-transition` використовується `incrementBannerTransition` без перевірки успішності перед перенаправленням користувача.

#### [banner-form.html](file:///home/coder/rfa/src/main/resources/templates/banner/banner-form.html)
* **Обмеження інтерфейсу (UX Limitation):**
  Компонент Dropzone (завантаження файлів) рендериться лише якщо `banner.id != null`:
  ```html
  <div th:if="${banner.id} != null" class="row">
  ```
  Оскільки `id` присвоюється лише безпосередньо перед записом у базу даних (у `@PrePersist`), при створенні нового банера (`/banners/new`) завантажувач прихований. Користувач змушений спочатку зберегти пустий текстовий банер, а потім знову відкрити його редагування для завантаження картинки чи аудіо.
  *Рекомендоване виправлення:* Ініціалізувати `id` у конструкторі або сервісі під час створення об'єкта, або розмежувати процеси створення та завантаження медіа.

#### [select-media-form.html](file:///home/coder/rfa/src/main/resources/templates/banner/select-media-form.html)
* **Використання аудіо-ендпоінту для прев'ю відео:**
  На лінії 93 також використовується шлях `/store/audio/` для прев'ю відео-банера з метою забезпечення підтримки HTTP Range стрімінгу. Рекомендується адаптувати це після додавання відповідного `/store/video/{uuid}` ендпоінту в контролері.

---

## 3. Резюме та рекомендації

1. **Виправлення критичних багів та доопрацювання API:**
   - Додати новий ендпоінт стрімінгу для відео (наприклад, `/store/video/...`) у [StoreSiteController](file:///home/coder/rfa/src/main/java/media/toloka/rfa/radio/store/StoreSiteController.java) з підтримкою HTTP Range/позиціонування, та перевести відео-банери в [banner.js](file:///home/coder/rfa/src/main/resources/static/js/banner/banner.js#L161) і [select-media-form.html](file:///home/coder/rfa/src/main/resources/templates/banner/select-media-form.html#L93) на використання цього нового ендпоінту.
   - Виправити ширину та висоту для `BANNER_TYPE_336_280px` на `336, 280` у [EBannerStyle.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/banner/model/enumerate/EBannerStyle.java#L6).
   - Додати перевірку на `null` об'єкта `banner` у методі `BannerMediaUpload` контролера [BannerDropPostFileController.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/banner/fileupload/BannerDropPostFileController.java#L69).

2. **Покращення безпеки (Security):**
   - Додати авторизаційну перевірку володіння банером у REST-контролерах. Клієнт повинен мати можливість маніпулювати (завантажувати медіа, оновлювати зв'язки) тільки тими банерами, де `banner.clientdetail` збігається з його власним профілем.

3. **Оптимізація продуктивності (Performance & Database):**
   - Уникати завантаження всіх банерів через `findAll()` у REST API. Слід додати методи фільтрації на рівні `BannerRepository` (наприклад, `findByApproveTrue()` або `findByApproveTrueAndBannertype(...)`).
   - Усунути подвійний виклик `bannerService.BannerGetPage` у [BannersController.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/banner/controller/BannersController.java#L60-L63).

4. **Покращення UX:**
   - Змінити логіку генерації `id` у [Banner.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/banner/model/Banner.java), щоб дозволити завантаження медіафайлів безпосередньо під час створення банера, або розробити комбіновану форму збереження.
