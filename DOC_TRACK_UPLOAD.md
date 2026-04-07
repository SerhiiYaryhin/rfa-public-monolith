# 📤 Завантаження та редагування треку — Документація

**Дата:** 2026-04-07  
**Гілка:** `ai`  
**Статус:** Готово до тестування

---

## 📋 Огляд

Функціонал дозволяє користувачу **завантажити аудіофайл** та **заповнити інформацію про трек** на одній сторінці. Раніше це були дві окремі сторінки: `/creater/trackupload` та `/creater/edittrack/{uuid}`.

### Архітектура

```
┌─────────────────────────────────────────────────────────────┐
│                    edittrack.html                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  РЕЖИМ 1: track.storeitem == null (файл відсутній)         │
│  ┌───────────────────────────────────────────────────────┐ │
│  │  🎵 Крок 1: Завантажте аудіофайл треку               │ │
│  │                                                       │ │
│  │  [═══ Dropzone Zone ═══]                             │ │
│  │  • Drag & Drop або вибір файлу                       │ │
│  │  • Валідація типу/розміру                            │ │
│  │  • CSRF токен в header                               │ │
│  └───────────────────────────────────────────────────────┘ │
│                                                             │
│  РЕЖИМ 2: track.storeitem != null (файл завантажено)       │
│  ┌───────────────────────────────────────────────────────┐ │
│  │  ✅ Аудіофайл: song.mp3  [▶ Прослухати]               │ │
│  │  Виконавець *: [________________]                     │ │
│  │  Назва треку *: [_______________]                     │ │
│  │  Стиль: [_______________]                             │ │
│  │  Опис *: [TinyMCE WYSIWYG редактор               ]   │ │
│  │  Альбом: [Single ▼]                                   │ │
│  │  [💾 Зберегти]  [← Назад до списку]                   │ │
│  └───────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔄 Потік даних

```
1. Користувач на сторінці /creater/tracks/0
      ↓ [натискає "Завантажити новий трек"]
2. GET /creater/newtrack
      ↓ [контролер створює порожній Track, повертає UUID]
3. redirect:/creater/edittrack/{uuid}
      ↓ [сторінка показує тільки Dropzone (режим 1)]
4. Користувач перетягує файл у Dropzone
      ↓ [Dropzone POST /creater/trackupload з CSRF header]
5. Контролер зберігає файл → створює Store → оновлює Track
      ↓ [response body = track.uuid]
6. Dropzone success callback читає UUID
      ↓ [redirect на ту саму сторінку]
7. GET /creater/edittrack/{uuid}
      ↓ [track.storeitem != null → показує форму (режим 2)]
8. Користувач заповнює поля → натискає "Зберегти"
      ↓ [POST /creater/edittrack з валідацією]
9. redirect:/creater/tracks/0 (список треків)
```

---

## 🧩 Компоненти

### Backend

#### 1. CreaterTrackController.java

| Метод | URL | Призначення |
|-------|-----|-------------|
| `createNewTrack()` | `GET /creater/newtrack` | Створює порожній Track → redirect на edittrack |
| `getCreaterEditTracks()` | `GET /creater/edittrack/{uuid}` | Показує форму (режим 1 або 2) |
| `postCreaterEditTracks()` | `POST /creater/edittrack` | Валідація + збереження інформації |

**Створення нового треку:**
```java
@GetMapping(value = "/creater/newtrack")
public String createNewTrack(Model model) {
    Users user = clientService.GetCurrentUser();
    Clientdetail cd = clientService.GetClientDetailByUser(user);

    Track track = new Track();
    track.setClientdetail(cd);
    track.setTochat(true);
    track.setNotnormalvocabulary(false);
    track.setApruve(false);
    track = createrService.SaveTrack(track);  // зберігає, повертає Track

    return "redirect:/creater/edittrack/" + track.getUuid();
}
```

**Відображення форми:**
```java
@GetMapping(value = "/creater/edittrack/{uuidTrack}")
public String getCreaterEditTracks(@PathVariable String uuidTrack, Model model) {
    Track track = createrService.GetTrackByUuid(uuidTrack);
    if (track == null) {
        return "redirect:/creater/tracks/0";
    }

    Store store = null;
    if (track.getStoreitem() != null) {
        store = storeService.GetStoreByUUID(track.getStoreitem().getUuid());
    }

    model.addAttribute("albumList", createrService.GetAllAlbumsByCreater(cd));
    model.addAttribute("track", track);
    model.addAttribute("store", store);

    return "/creater/edittrack";
}
```

**Валідація при збереженні:**
```java
@PostMapping(value = "/creater/edittrack")
public String postCreaterEditTracks(@ModelAttribute Track ftrack, ...) {
    // Валідація обов'язкових полів
    String autor = StringUtils.hasText(ftrack.getAutor()) ? ftrack.getAutor().trim() : "";
    String name = StringUtils.hasText(ftrack.getName()) ? ftrack.getName().trim() : "";
    String description = StringUtils.hasText(ftrack.getDescription()) ? ftrack.getDescription().trim() : "";

    // Перевірка мінімальної довжини опису (60 символів)
    if (description.length() < MIN_DESCRIPTION_LENGTH) {
        redirectAttributes.addFlashAttribute("validationError", "Опис занадто короткий...");
        return "redirect:/creater/edittrack/" + track.getUuid();
    }

    // Збереження
    track.setName(name);
    track.setDescription(description);
    // ...
    createrService.SaveTrack(track);
    return "redirect:/creater/tracks/" + curpage;
}
```

#### 2. CreaterDropPostFileController.java

| Метод | URL | Призначення |
|-------|-----|-------------|
| `uploadTrack()` | `POST /creater/trackupload` | Завантаження файлу, повернення UUID треку |

```java
@PostMapping(path = "/creater/trackupload")
public void uploadTrack(@RequestParam("file") MultipartFile file,
                        HttpServletResponse response) throws IOException {
    // 1. Перевірка файлу
    if (file.isEmpty()) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().write("Файл порожній");
        return;
    }

    // 2. Перевірка прав
    Clientdetail cd = clientService.GetClientDetailByUser(clientService.GetCurrentUser());
    if (!clientService.ClientCanDownloadFile(cd)) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.getWriter().write("Немає прав");
        return;
    }

    // 3. Збереження файлу в Store
    String storeUUID = storeService.PutFileToStore(
        file.getInputStream(), file.getOriginalFilename(), cd, STORE_TRACK
    );

    // 4. Створення/оновлення треку
    Track track = createrService.SaveTrackUploadInfo(storeUUID, cd);

    // 5. Повернення UUID треку в response body
    response.setContentType("text/plain");
    response.getWriter().write(track.getUuid());
}
```

#### 3. CreaterService.java

```java
public Track SaveTrackUploadInfo(String storeitemUUID, Clientdetail cd) {
    Track track = new Track();
    track.setStatus(EDocumentStatus.STATUS_LOADED);
    track.setClientdetail(cd);
    track.setStoreuuid(storeitemUUID);
    track.setTochat(false);
    track.setStoreitem(storeService.GetStoreByUUID(storeitemUUID));
    track = trackRepository.save(track);
    return track;  // ← повертаємо Track для отримання UUID
}
```

---

### Frontend

#### edittrack.html — структура шаблону

**Режим 1: Файл відсутній (Dropzone)**
```html
<div th:if="${track.storeitem == null}">
    <div class="card border-warning">
        <div class="card-header bg-warning text-dark">
            🎵 Крок 1: Завантажте аудіофайл треку
        </div>
        <div class="card-body">
            <div id="track-dropzone" class="dropzone">
                <div class="dz-message needsclick">
                    <button class="dz-button">Перетягніть файл сюди...</button>
                </div>
            </div>
        </div>
    </div>
</div>
```

**Режим 2: Файл завантажено (форма редагування)**
```html
<div th:if="${track.storeitem != null}">
    <form th:action="@{/creater/edittrack}" method="post" th:object="${track}">
        <input type="hidden" th:field="*{id}"/>

        <!-- Повідомлення про успішне завантаження файлу -->
        <div class="alert alert-success">
            ✅ Аудіофайл: <span th:text="${track.storeitem.filename}"></span>
        </div>

        <!-- Поля форми -->
        <div class="mb-3">
            <label for="autor">Виконавець *</label>
            <input th:field="*{autor}" type="text" class="form-control">
        </div>

        <!-- TinyMCE для опису -->
        <div class="mb-3">
            <label for="track-description">Опис треку *</label>
            <textarea th:field="*{description}" id="track-description"></textarea>
        </div>

        <button type="submit" class="btn btn-primary">Зберегти</button>
    </form>
</div>
```

---

#### Dropzone конфігурація

```javascript
// Отримуємо CSRF токен з мета-тегів
var csrfToken = document.querySelector('meta[name="_csrf"]').content;
var csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

$("#track-dropzone").dropzone({
    // Основні
    url: "/creater/trackupload",
    paramName: "file",           // @RequestParam("file")
    maxFilesize: 100,            // MB
    acceptedFiles: "audio/*,.mp3,.wav,.ogg,.flac,.aac,.wma",

    // CSRF авторизація
    headers: { [csrfHeader]: csrfToken },

    // Українські повідомлення
    dictDefaultMessage: "Перетягніть аудіофайл сюди...",
    dictInvalidFileType: "Непідтримуваний тип файлу",
    dictFileTooBig: "Файл занадто великий (макс. {{maxFilesize}} MB)",
    dictResponseError: "Помилка сервера",

    // Успіх — читаємо UUID з response body
    success: function(file, responseText) {
        var trackUuid = (typeof responseText === 'string') ? responseText.trim() : '';
        if (trackUuid && isValidUuid(trackUuid)) {
            toastr.success('Файл "' + file.name + '" завантажено!');
            setTimeout(function() {
                window.location.href = '/creater/edittrack/' + trackUuid;
            }, 1000);
        } else {
            toastr.error('Помилка: ' + responseText);
        }
    },

    // Помилка
    error: function(file, errorMessage) {
        toastr.error('Помилка: ' + (errorMessage || 'Невідома помилка'));
    },

    // Логування
    addedfile: function(file) {
        console.log('Додано:', file.name, (file.size / 1024 / 1024).toFixed(2), 'MB');
    }
});
```

---

## 🔐 CSRF захист

Dropzone відправляє AJAX запити, тому CSRF токен потрібно додавати вручну:

**1. Мета-теги в `<head>`:**
```html
<meta name="_csrf" th:content="${_csrf.token}"/>
<meta name="_csrf_header" th:content="${_csrf.headerName}"/>
```

**2. Dropzone headers:**
```js
headers: { [csrfHeader]: csrfToken }
```

Без цього Spring Security повертає **405 Method Not Allowed**.

---

## ✅ Валідація

| Поле | Правило | Повідомлення |
|------|---------|-------------|
| `autor` | Не порожній | "Поле «Виконавець» є обов'язковим" |
| `name` | Не порожній | "Поле «Назва треку» є обов'язковим" |
| `description` | ≥ 60 символів | "Опис занадто короткий. Мінімум 60 символів (зараз: X)" |

При помилці — redirect назад з flash-повідомленням:
```html
<div th:if="${validationError}" class="alert alert-danger">
    <span th:utext="${validationError}"></span>
</div>
```

---

## 🗃️ Модель даних

### Track entity

| Поле | Тип | Опис |
|------|-----|------|
| `id` | Long | Primary Key |
| `uuid` | String | UUID для URL |
| `name` | String | Назва треку |
| `autor` | String | Виконавець |
| `description` | TEXT | Опис (TinyMCE HTML) |
| `style` | String | Жанр/стиль |
| `storeitem` | Store (OneToOne) | Посилання на Store (аудіофайл) |
| `album` | Album (ManyToOne) | Альбом |
| `clientdetail` | Clientdetail | Автор |
| `apruve` | Boolean | Схвалено для публікації |
| `tochat` | Boolean | Опублікувати в чаті |
| `notnormalvocabulary` | Boolean | Ненормативна лексика |

---

## 📁 Змінені файли

| Файл | Зміни |
|------|-------|
| `creater/edittrack.html` | Повністю переписано — 2 режими + Dropzone |
| `creater/tracks.html` | Додано кнопку "Завантажити новий трек" |
| `creater/CreaterTrackController.java` | Додано `createNewTrack()`, валідацію |
| `creater/fileupload/CreaterDropPostFileController.java` | Повертає UUID треку |
| `creater/service/CreaterService.java` | `SaveTrackUploadInfo()` повертає Track |
| `fragments/creater.html` | Додано null check для `track.storeitem` |
| `store/StoreSiteController.java` | Додано null check для `storeRecord` |

---

## 🐛 Відомі проблеми та вирішення

| Проблема | Причина | Вирішення |
|----------|---------|-----------|
| 405 помилка при завантаженні | CSRF токен не відправлявся | Додано `headers: { [csrfHeader]: csrfToken }` |
| NPE `track.storeitem.uuid` | Трек без файлу | Додано `th:if="${track.storeitem != null}"` |
| NPE `storeRecord.getFilepatch()` | Store не знайдено за UUID | Додано null check з exception |
| Dropzone відтворює файл в браузері | Конфлікт з audio MIME | Dropzone перехоплює подію — працює коректно |

---

*Документ створено автоматично. Останнє оновлення: 2026-04-07*
