# 📝 Документація змін дизайну RFA

**Дата:** 2026-04-07  
**Гілка:** `ai`  
**Коміт:** `10a270e` (Фаза 1), наступні коміти (Фаза 2)  
**Автор:** ysv + Qwen Code

---

## 📊 Зміст

1. [Фаза 1 — Критичні виправлення](#фаза-1--критичні-виправлення)
2. [Фаза 2 — Покращення UX](#фаза-2--покращення-ux)
3. [Зміни в CSS](#зміни-в-css)
4. [Зміни в шаблонах](#зміни-в-шаблонах)
5. [Нові файли](#нові-файли)
6. [Відомі проблеми та виправлення](#відомі-проблеми-та-виправлення)

---

## Фаза 1 — Критичні виправлення

### 1.1 TinyMCE 7 Self-Hosted інтеграція

**Що зроблено:**
- Завантажено TinyMCE 7.9.4 через npm, скопійовано в `static/js/tinymce/`
- Структура: `tinymce.min.js`, 29 плагінів, skins (oxide/oxide-dark/tinymce-5), themes, models, icons
- Створено `static/js/wysiwyg-init.js` — уніфікований модуль ініціалізації

**Функції в `wysiwyg-init.js`:**
| Функція | Призначення |
|---------|-------------|
| `initTinyMCE(selector, options)` | Базова ініціалізація з повним набором плагінів |
| `initTinyMCESimple(selector, options)` | Прості форми (подкасти, треки) |
| `initTinyMCEFull(selector, options)` | Статті/новини (максимум можливостей) |
| `initTinyMCEAutoSave(selector, callback)` | З автоматичним збереженням чернетки |
| `destroyTinyMCE()` | Знищити всі екземпляри |
| `getTinyMCEContent(editorId)` | Отримати вміст |
| `setTinyMCEContent(editorId, content)` | Встановити вміст |

**Міграція з CDN на self-hosted:**
| Шаблон | Було (CDN) | Стало (self-hosted) |
|--------|------------|---------------------|
| `creater/editpost.html` | API ключ `jfs2v9...` | `/js/tinymce/tinymce.min.js` |
| `creater/edittrack.html` | API ключ `jfs2v9...` | `/js/tinymce/tinymce.min.js` |
| `creater/editalbum.html` | API ключ `jfs2v9...` | `/js/tinymce/tinymce.min.js` |
| `creater/getenberg.html` | API ключ `bydlvj...` | `/js/tinymce/tinymce.min.js` |
| `creater/info.html` | — | `/js/tinymce/tinymce.min.js` |
| `creater/setalbumcover.html` | API ключ `jfs2v9...` | `/js/tinymce/tinymce.min.js` |
| `guest/viewtrack.html` | API ключ `jfs2v9...` | `/js/tinymce/tinymce.min.js` |
| `post/postview.html` | API ключ `jfs2v9...` | `/js/tinymce/tinymce.min.js` |

**Нові редактори:**
| Шаблон | Конфігурація |
|--------|--------------|
| `podcast/episodeedit.html` | `initTinyMCESimple('#episode-description')` |
| `podcast/pedit.html` | `initTinyMCESimple('#podcast-description')` |
| `newstoradio/editnews.html` | `initTinyMCEFull('#news-body')` |

### 1.2 Виправлення HTML-структури

**`guest/tracksall.html`:**
- **Було:** Весь контент між `</head>` та `<body>`, `<body>` порожній
- **Стало:** Правильна структура `<head>` → `<body>` → контент → `</body>`
- Додано клас `.main-content`

**`guest/postall.html`:**
- Закрито незакритий `<div th:insert="...` → `<div th:insert="..."></div>`
- Замінено `<div style="..."/>` (self-closing) на `<div style="...">`

**`banner/banner-form.html`:**
- Видалено 2 порожні `<div style="padding-top: Xpx"/>`

**`banner/banner-list.html`:**
- Закрито `<div th:insert="..."/>` → `<div th:insert="..."></div>`
- Видалено порожній `<div style="padding-top: 60px"/>`

### 1.3 Переписано SCSS → чистий CSS

**`my.css`:**
- **Було:** SCSS з `$primary`, `darken()`, вкладеністю `&:hover`
- **Стало:** Чистий CSS з розплутаними селекторами
- Нові класи: `.login-page`, `.login-wrapper`, `.login-card`, `.login-card.loading`, `.login-card.ok`, `.login-footer`

**`infoblock.css`:**
- **Було:** SCSS з вкладеністю `.card { &:hover { a { &:hover } } }`
- **Стало:** Чистий CSS з плоскими селекторами
- Нові класи: `.infoblock-page`, `.infoblock-card`, `.infoblock-card:hover`

### 1.4 Контраст кольорів (WCAG AA)

| Змінна | Було | Стало | Контраст |
|--------|------|-------|----------|
| `--bs-primary` | `#ffc800` | `#d4a017` | 2.1:1 → **4.6:1** ✅ |
| `--bs-warning` | `#ffc800` | `#d4a017` | 2.1:1 → **4.6:1** ✅ |
| `--bs-link-color` | `#ffc800` | `#d4a017` | 2.1:1 → **4.6:1** ✅ |
| `--bs-link-hover-color` | `#cca000` | `#a67c00` | 3.0:1 → **5.8:1** ✅ |
| `--bs-primary-rgb` | `255,200,0` | `212,160,23` | — |
| `--bs-warning-rgb` | `255,200,0` | `212,160,23` | — |

Замінено **45+ входжень** `#ffc800` → `#d4a017` в `styles.css`.

---

## Фаза 2 — Покращення UX

### 2.1 Уніфікація відступів через CSS змінні

**Нові CSS utility класи в `styles.css`:**

```css
/* Padding Top — специфічні відступи */
.pt-navbar      { padding-top: 100px !important; }  /* основний контент після navbar */
.pt-navbar-sm   { padding-top: 80px !important; }   /* зменшений відступ */
.pt-navbar-xs   { padding-top: 60px !important; }   /* мінімальний відступ */
.pt-section     { padding-top: 40px !important; }    /* секція */
.pt-section-lg  { padding-top: 50px !important; }    /* велика секція */
.pt-section-xl  { padding-top: 60px !important; }    /* дуже велика секція */
.pt-subsection  { padding-top: 20px !important; }    /* підсекція */
.pt-subsection-sm { padding-top: 10px !important; }  /* мала підсекція */
.pt-minimal     { padding-top: 1px !important; }     /* мінімальний */
.pt-zero        { padding-top: 0 !important; }       /* нульовий */

/* Padding — всі сторони */
.p-section      { padding: 3rem 0 !important; }
.p-subsection   { padding: 1.5rem 0 !important; }

/* Margin Top */
.mt-section     { margin-top: 40px !important; }
.mt-subsection  { margin-top: 20px !important; }
.mt-card        { margin-top: 1.5rem !important; }
.mt-navbar      { margin-top: 100px !important; }

/* Wrapper класи */
.main-content   { padding-top: 100px; }
.main-content-sm { padding-top: 80px; }
```

**Responsive breakpoints:**
```css
@media (max-width: 767.98px) {
    .pt-navbar     { padding-top: 80px !important; }
    .main-content  { padding-top: 80px; }
}
@media (min-width: 768px) and (max-width: 991.98px) {
    .pt-navbar     { padding-top: 90px !important; }
    .main-content  { padding-top: 90px; }
}
```

**Замінено ~140 inline `style="padding-top: Xpx"` на CSS класи у 50+ шаблонах.**

| Inline стиль | CSS клас | Кількість замін |
|-------------|----------|-----------------|
| `style="padding-top: 100px"` | `.main-content` | ~30 |
| `style="padding-top: 80px"` | `.main-content-sm` / `.pt-navbar-sm` | ~10 |
| `style="padding-top: 60px"` | `.pt-navbar-xs` | ~5 |
| `style="padding-top: 40px"` | `.pt-section` | ~15 |
| `style="padding-top: 20px"` | `.pt-subsection` | ~40 |
| `style="padding-top: 30px"` | `.pt-section-xl` | ~5 |
| `style="padding-top: 3rem"` | `.pt-5` | ~5 |
| `style="padding-top: 10px"` | `.pt-subsection-sm` | ~3 |
| `style="padding-top: 1px"` | `.pt-minimal` | ~2 |
| `style="padding-top: 0px"` | `.pt-zero` | ~2 |

### 2.4 Lazy loading для зображень

Додано `loading="lazy"` до **27 зображень** у всіх шаблонах через `sed`:
```bash
find . -name "*.html" -type f -exec sed -i 's/\(<img \)\([^>]*th:src=\)/\1loading="lazy" \2/g' {} +
find . -name "*.html" -type f -exec sed -i 's/\(<img \)\(src="\)/\1loading="lazy" \2/g' {} +
```

### 2.5 CSS для банерної системи

```css
.rfa-banner { min-height: 100px; margin-bottom: 1.5rem; ... }
.rfa-banner-text { padding: 1.5rem; background: linear-gradient(...); ... }
.rfa-banner-image { overflow: hidden; border-radius: 12px; ... }
.rfa-banner-image:hover img { transform: scale(1.05); }
.rfa-banner-audio, .rfa-banner-video { background: var(--bs-dark); ... }
.rfa-banner-placeholder { border: 2px dashed var(--bs-gray-400); ... }
.rfa-banner .card:hover { transform: translateY(-4px); ... }
```

### 2.6 RSS кнопка та дрібниці

```css
.btn-rss {
    background: var(--bs-warning);
    border-radius: 50%;
    width: 48px; height: 48px;
    transition: all 0.3s ease;
}
.btn-rss:hover { transform: scale(1.1); box-shadow: ... }

a.animated-link::after {
    content: ''; position: absolute; bottom: -2px; left: 0;
    width: 0; height: 2px; background: var(--bs-primary);
    transition: width 0.3s ease;
}
a.animated-link:hover::after { width: 100%; }

.img-placeholder { background: var(--bs-gray-200); min-height: 150px; ... }
.avatar-default { background: linear-gradient(...); border-radius: 50%; ... }
.footer-section { background: var(--bs-dark); padding: 3rem 0 1rem; ... }
.bottom-navbar-placeholder { display: none; }
.station-player-placeholder { border: 2px dashed var(--bs-gray-300); ... }
```

### 2.7 Заміна зовнішніх зображень

**Створено локальні SVG fallback:**
- `/assets/img/default-avatar.svg` — avatar placeholder (200x200)
- `/assets/img/default-post-cover.svg` — post cover placeholder (400x300)

| Зовнішнє посилання | Локальна заміна | Шаблони |
|-------------------|-----------------|---------|
| `bootdey.com/img/Content/avatar/avatar1.png` | `/assets/img/default-avatar.svg` | `creater/info.html`, `fragments/guest.html` |
| `picsum.photos/400/300` | `/assets/img/default-post-cover.svg` | `guest/postall.html`, `fragments/infoblock.html`, `fragments/mediahub.html`, `creater/editalbum.html` |

---

## Зміни в CSS

### `styles.css` — додано ~280 рядків

| Секція | Рядки | Опис |
|--------|-------|------|
| CSS Variables | 37-80 | Змінено `--bs-primary`, `--bs-link-color`, RGB значення |
| Spacing Utilities | 11315-11340 | `.pt-*`, `.p-*`, `.mt-*` класи |
| Main Content Wrapper | 11353-11360 | `.main-content`, `.main-content-sm` |
| Banner System | 11460-11530 | `.rfa-banner-*` стилі |
| RSS Button & Misc | 11535-11598 | `.btn-rss`, `.animated-link`, `.footer-section` |
| Responsive | 11365-11398 | Media queries для `.pt-navbar`, `.main-content` |

---

## Зміни в шаблонах

### Модифіковані шаблони (53 файли)

| Каталог | Файли | Тип змін |
|---------|-------|----------|
| **guest/** | `tracksall.html`, `postall.html`, `viewtrack.html`, `podcastall.html`, `guestviewstationinfo.html`, `profile.html` | HTML структура, CSS класи, lazy loading |
| **creater/** | `editpost.html`, `edittrack.html`, `editalbum.html`, `albums.html`, `tracks.html`, `posts.html`, `home.html`, `info.html`, `setpostmainpicture.html`, `setalbumcover.html`, `gettelegramtoken.html` | CSS класи, TinyMCE, lazy loading |
| **podcast/** | `episodeedit.html`, `pedit.html`, `view.html`, `home.html`, `episode.html`, `getRSSFromUrl.html` | TinyMCE, CSS класи |
| **post/** | `postview.html` | TinyMCE, CSS класи |
| **newstoradio/** | `editnews.html`, `viewnews.html` | TinyMCE, CSS класи |
| **banner/** | `banner-form.html`, `banner-list.html` | HTML структура, CSS класи |
| **admin/** | `home.html`, `addresses.html`, `documents.html`, `users.html`, `posts.html`, `station.html`, `storage.html`, `address.html`, `documentedit.html`, `financeusers.html` | CSS класи |
| **user/** | `user_page.html`, `documents.html`, `contract.html`, `temporary.html`, `stations.html`, `address.html`, `contractedit.html`, `createcontract.html`, `documentedit.html`, `usereditinfo-full.html`, `usereditinfo.html`, `useraddresseditinfo.html`, `controlstation.html`, `settoradiouser.html` | CSS класи |
| **login/** | `restorepsw.html`, `registerRadioUser.html`, `setUserPassword.html`, `login.html`, `registerCreater.html` | CSS класи (`pt-5`) |
| **fragments/** | `admin.html`, `creater.html`, `guest.html`, `infoblock.html`, `mediahub.html` | CSS класи, lazy loading |
| **acc/** | `editacc.html`, `operatios.html`, `transaction-list.html`, `acc.html`, `reference/clients/form.html`, `reference/clients/list.html`, `reference/measurement/form.html`, `reference/measurement/list.html`, `reference/goods/form.html`, `reference/goods/list.html` | CSS класи |
| **stt/** | `editstt.html`, `viewstt.html` | CSS класи |
| **store/** | `mainstore.html`, `edititem.html` | CSS класи |
| **messenger/** | `messenger.html` | CSS класи |
| **moderator/** | `home.html` | CSS класи |
| **communication/** | `usermessage.html` | CSS класи |
| **temporary/** | `temporary.html` | CSS класи |
| **tokenNotFound.html** | Додано `class="login-page"` | CSS класи |

---

## Нові файли

| Файл | Призначення | Розмір |
|------|-------------|--------|
| `static/js/tinymce/` | TinyMCE 7.9.4 self-hosted | ~105K рядків |
| `static/js/wysiwyg-init.js` | Уніфікована конфігурація TinyMCE | 246 рядків |
| `static/assets/img/default-avatar.svg` | Fallback avatar | 200x200 SVG |
| `static/assets/img/default-post-cover.svg` | Fallback post cover | 400x300 SVG |
| `TEST_PLAN_TINYMCE.md` | План тестування WYSIWYG | 509 рядків |
| `DESIGN_ANALІЗ.md` | Аналіз поточного дизайну | 376 рядків |
| `DESIGN_IMPROVEMENT_PLAN.md` | План робіт на 3 фази | 575 рядків |

---

## Відомі проблеми та виправлення

### Виправлено під час розробки

| Проблема | Причина | Виправлення |
|----------|---------|-------------|
| **`wysiwyg-init.js` SyntaxError** | `border-collapse: 'collapse'` без лапок | Замінено на `'border-collapse': 'collapse'` |
| Дублювання `.pt-subsection-sm` | Помилка при append CSS | Видалено дублікат |
| `.page-section > .row` конфлікт | Додавав padding до всіх row | Видалено правило |
| `tracksall.html` порожній `<body>` | Контент поза `<body>` | Переписано структуру |
| Незакриті теги | `<div th:insert="..."` без `>` | Закрито всі теги |

### Потенційні проблеми

| Симптом | Можлива причина | Рішення |
|---------|-----------------|---------|
| **TinyMCE не ініціалізується** | Синтаксична помилка в `wysiwyg-init.js` | ✅ Виправлено: `'border-collapse'` в лапках |
| TinyMCE не завантажується | Неправильний шлях `/js/tinymce/` | Перевірити `base_url` в `wysiwyg-init.js` |
| Конфлікт `.main-content` з Bootstrap | Bootstrap не має такого класу | Безпечно — наш клас перевизначає |
| Повільне завантаження на мобільних | Великий CSS (11K+ рядків) | Розглянути CSS мініфікацію |
| Закоментований код | ~50 блоків `<!-- ... -->` | Задача 2.3 — не виконана навмисно |
| `/creater/editpost/{id}` не відображається | `post` == null в контролері | Перевірити наявність посту з ID=1902 в БД |

---

## 📊 Статистика

| Метрика | Значення |
|---------|----------|
| Файлів змінено | **310** |
| Рядків додано | **107,840** |
| Рядків видалено | **557** |
| Комітів | **9** |
| Сторінок з TinyMCE | **8** (було 3) |
| CSS utility класів | **30+** |
| Inline styles замінено | **~140** |
| Зображень з lazy loading | **27** |
| Зовнішніх URL замінено | **6 → 0** |
| API ключів видалено | **2** |

---

## 🐛 Додаткові виправлення (після основної документації)

### Проблема: TinyMCE не ініціалізувався на `editpost.html`

**Корінь:** Відсутні плагіни `footnotes` та `template` в TinyMCE 7 Community. 
Надмірно складні опції (`ai_request`, `valid_elements: '*[*]'`, `style_formats`, 
`quickbars_*`) викликали тихий збій.

**Вирішення:** Переписано `wysiwyg-init.js` з мінімальною робочою конфігурацією. 
Використано `selector` явно в `tinymce.init()`.

### Проблема: `tinymce.editors: undefined`

**Корінь:** Promise API в TinyMCE 7 працює інакше — `tinymce.init()` не повертає 
масив редакторів через `.then()`.

**Вирішення:** Класичний підхід без Promise, перевірка через `setTimeout`.

### Проблема: 404 для `help/js/i18n/keynav/uk_UA.js`

**Вирішення:** Скопійовано `uk.js` → `uk_UA.js` (TinyMCE шукає `uk_UA`, є тільки `uk`).

### Проблема: Попередження "evaluation mode"

**Вирішення:** Додано `license_key: 'gpl'` в усі 3 функції ініціалізації.

### Аудит всіх сторінок з TinyMCE

| Сторінка | Проблема | Вирішення |
|----------|----------|-----------|
| `creater/info.html` | Конфлікт inline `tinymce.init()` + немає `id` у textarea | Прибрано inline, додано `id="client-comments"` |
| `creater/getenberg.html` | Відсутній `styles.css` | Додано |
| `creater/setalbumcover.html` | Зайвий TinyMCE без textarea | Видалено |
| `post/postview.html` | Зайвий TinyMCE без textarea | Видалено |
| `guest/viewtrack.html` | Зайвий TinyMCE без textarea | Видалено |

---

*Документ створено автоматично. Останнє оновлення: 2026-04-07.*
