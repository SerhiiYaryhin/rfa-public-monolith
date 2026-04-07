# 📋 План покращення дизайну відкритої частини сайту RFA

**Версія:** 1.0  
**Дата створення:** 2026-04-07  
**Статус:** Затвердження

---

## Огляд

Цей план описує послідовність робіт з покращення візуального дизайну, UX та доступності публічної частини сайту радіостанції "Толока". Роботи розподілені на **3 фази** за пріоритетністю.

---

## Фаза 1: 🔴 Критичні виправлення

> **Мета:** Усунути блокуючі помилки, що впливають на відображення та доступність.

---

### Задача 1.1: Виправити HTML-структуру `tracksall.html`

**Проблема:** Весь контент розташований між `</head>` та `<body>`, тег `<body>` порожній.  
**Вплив:** Сторінка може некоректно рендеритися в браузерах.

**Кроки:**
1. Перемістити navbar та контент всередину `<body>`
2. Переконатись, що `<head>` містить тільки мета-теги, CSS та `<title>`
3. Перевірити валідність через W3C Validator

**Файли:** `src/main/resources/templates/guest/tracksall.html`  
**Оцінка:** 30 хв

---

### Задача 1.2: Закрити незакриті теги

**Проблема:** `postall.html` має `<div th:insert="..."` без закриваючого `>`.

**Кроки:**
1. Знайти всі незакриті теги в шаблонах
2. Закрити їх правильно
3. Перевірити валідність

**Файли:** `src/main/resources/templates/guest/postall.html` + інші  
**Оцінка:** 20 хв

---

### Задача 1.3: Переписати SCSS-синтаксис на чистий CSS

**Проблема:** `my.css` та `infoblock.css` використовують SCSS-змінні (`$primary`) та вкладеність (`&`), що не працює в браузері.

**Кроки:**
1. `my.css` — замінити `$primary: #2196F3` на `var(--primary)` або хардкод
2. `my.css` — розплутати вкладені селектори `&:hover` → `.login-box:hover`
3. `infoblock.css` — аналогічно
4. Перевірити відображення сторінок логіну/реєстрації

**Файли:**
- `src/main/resources/static/css/my.css`
- `src/main/resources/static/css/infoblock.css`

**Оцінка:** 1 год

---

### Задача 1.4: Виправити контраст кольорів (WCAG AA)

**Проблема:** `#ffc800` на білому фоні = 2.1:1 (потрібно мінімум 4.5:1).

**Кроки:**
1. Змінити основний колір посилань з `#ffc800` на `#d4a017`
2. Зберегти `#ffc800` для великих елементів (кнопки, іконки > 18px)
3. Оновити `--bs-primary` в CSS змінних
4. Перевірити контраст для всіх комбінацій текст/фон

**Файли:** `src/main/resources/static/css/styles.css`  
**Оцінка:** 45 хв

**Приймальні критерії:**
- ✅ Усі тексти мають контраст ≥ 4.5:1
- ✅ Великі елементи мають контраст ≥ 3:1
- ✅ Перевірено через Lighthouse Accessibility

---

### ✅ Критерії завершення Фази 1

- [ ] `tracksall.html` проходить HTML-валідацію
- [ ] Жодного незакритого тегу в шаблонах
- [ ] `my.css` та `infoblock.css` працюють в браузері
- [ ] Lighthouse Accessibility ≥ 75
- [ ] Жодного WCAG AA порушення контрасту

---

## Фаза 2: 🟡 Покращення UX

> **Мета:** Уніфікувати стилі, покращити продуктивність, прибрати технічний борг.

---

### Задача 2.1: Уніфікувати відступи через CSS змінні

**Проблема:** Inline `style="padding-top: Xpx"` з різними значеннями на кожній сторінці.

**Кроки:**
1. Створити CSS змінні:
```css
:root {
    --navbar-height: 70px;
    --content-pt: calc(var(--navbar-height) + 1rem);
    --content-pt-sm: calc(var(--navbar-height) + 2rem);
    --section-padding: 4rem 0;
}
```
2. Замінити всі `style="padding-top: 100px"` на `class="main-content"`
3. Замінити `style="padding-top: 80px"` тощо

**Файли:** Всі HTML-шаблони + `styles.css`  
**Оцінка:** 2 год

---

### Задача 2.2: Винести inline стилі в CSS

**Проблема:** Десятки inline `style` атрибутів ускладнюють підтримку.

**Кроки:**
1. Знайти всі inline стилі (grep: `style="` в шаблонах)
2. Створити CSS-класи для кожного патерну:
```css
/* Замість style="background-color:#F0F0F0; border-radius: 15px" */
.carousel-item-inner {
    background-color: #f0f0f0;
    border-radius: 15px;
}
```
3. Замінити inline на класи

**Файли:** Всі HTML-шаблони  
**Оцінка:** 3 год

---

### Задача 2.3: Прибрати закоментований код

**Проблема:** Десятки блоків `<!-- ... -->` забруднюють шаблони.

**Кроки:**
1. Знайти всі закоментовані блоки
2. Видалити ті, що не потрібні
3. Зберегти важливі примітки як коментарі розробника (не HTML)

**Команда для пошуку:**
```bash
grep -r "<!--.*-->" templates/ | grep -v "TODO\|FIXME\|NOTE" | wc -l
```

**Файли:** Всі HTML-шаблони  
**Оцінка:** 2 год

---

### Задача 2.4: Додати lazy loading для зображень

**Проблема:** Всі зображення завантажуються одразу, навіть поза viewport.

**Кроки:**
1. Додати `loading="lazy"` до всіх `<img>`:
```html
<img th:src="..." loading="lazy" alt="...">
```
2. Для фонових зображень використати Intersection Observer
3. Додати placeholder для зображень, що завантажуються

**Файли:** Всі HTML-шаблони з `<img>`  
**Оцінка:** 1.5 год

---

### Задача 2.5: Створити CSS для банерної системи

**Проблема:** `.rfa-banner-*` класи порожні, банери завантажуються через JS без стилів.

**Кроки:**
1. Створити `static/css/components/banners.css`:
```css
.rfa-banner {
    min-height: 100px;
    margin-bottom: 1.5rem;
    display: flex;
    align-items: center;
    justify-content: center;
}

.rfa-banner-text {
    padding: 1.5rem;
    background: linear-gradient(135deg, var(--color-primary-light), var(--color-accent));
    border-radius: 12px;
    color: var(--color-dark);
}

.rfa-banner-image {
    overflow: hidden;
    border-radius: 12px;
}

.rfa-banner-image img {
    width: 100%;
    height: auto;
    transition: transform 0.3s ease;
}

.rfa-banner-image:hover img {
    transform: scale(1.05);
}

.rfa-banner-audio,
.rfa-banner-video {
    background: var(--color-dark);
    border-radius: 12px;
    padding: 1rem;
}
```
2. Підключити до `styles.css` або окремо в шаблонах

**Файли:** Новий `banners.css` + `postview.html` та інші  
**Оцінка:** 1.5 год

---

### Задача 2.6: Стилізувати RSS кнопку та інші дрібниці

**Проблема:** RSS кнопка — стандартна кнопка браузера.

**Кроки:**
1. Стилізувати RSS кнопку:
```css
.btn-rss {
    background: var(--color-accent);
    color: white;
    border: none;
    border-radius: 50%;
    width: 40px;
    height: 40px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    transition: all 0.3s ease;
}
.btn-rss:hover {
    background: var(--color-accent-dark);
    transform: scale(1.1);
}
```
2. Прибрати placeholder "Тут буде плеєр станції" або реалізувати
3. Видалити/реалізувати bottom navbar

**Файли:** `podcast/view.html`, `guest/guestviewstationinfo.html`  
**Оцінка:** 1 год

---

### Задача 2.7: Замінити зовнішні залежності зображень

**Проблема:** `bootdey.com`, `picsum.photos` — працюють тільки з інтернетом.

**Кроки:**
1. Створити локальні fallback-зображення:
```
static/assets/img/
├── default-avatar.png      # Замість bootdey.com avatar
├── default-post-cover.png  # Замість picsum.photos
└── default-album-cover.png # Замість picsum.photos
```
2. Оновити шаблони:
```html
<img th:src="${user.avatar != null ? user.avatar : '/assets/img/default-avatar.png'}" />
```

**Файли:** Нові зображення + шаблони  
**Оцінка:** 1.5 год

---

### ✅ Критерії завершення Фази 2

- [ ] Жодного inline `style="padding-top: ..."` в шаблонах
- [ ] Жодного inline style, який можна винести в CSS
- [ ] Закоментований код видалено
- [ ] Всі зображення мають `loading="lazy"`
- [ ] Банери мають CSS-стилі
- [ ] RSS кнопка стилізована
- [ ] Жодного зовнішнього зображення-залежності
- [ ] Lighthouse Performance ≥ 75

---

## Фаза 3: 🟢 Візуальні покращення

> **Мета:** Сучасний вигляд, анімації, темна тема, покращення бренду.

---

### Задача 3.1: Додати анімації та hover-ефекти

**Кроки:**
1. Створити `static/css/animations.css`:
```css
/* Fade in on scroll */
.fade-in {
    opacity: 0;
    transform: translateY(20px);
    transition: opacity 0.6s ease, transform 0.6s ease;
}
.fade-in.visible {
    opacity: 1;
    transform: translateY(0);
}

/* Card hover */
.card {
    transition: transform 0.3s ease, box-shadow 0.3s ease;
}
.card:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 24px rgba(0, 0, 0, 0.15);
}

/* Link underline animation */
a {
    position: relative;
    text-decoration: none;
}
a::after {
    content: '';
    position: absolute;
    bottom: -2px;
    left: 0;
    width: 0;
    height: 2px;
    background: var(--color-primary);
    transition: width 0.3s ease;
}
a:hover::after {
    width: 100%;
}

/* Button ripple */
.btn {
    position: relative;
    overflow: hidden;
}
.btn::after {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 0;
    height: 0;
    background: rgba(255, 255, 255, 0.3);
    border-radius: 50%;
    transform: translate(-50%, -50%);
    transition: width 0.6s ease, height 0.6s ease;
}
.btn:active::after {
    width: 300px;
    height: 300px;
}
```
2. Додати JS для scroll-анімацій:
```javascript
document.addEventListener('DOMContentLoaded', function() {
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('visible');
            }
        });
    });

    document.querySelectorAll('.fade-in').forEach(el => observer.observe(el));
});
```

**Файли:** Новий `animations.css` + JS + шаблони  
**Оцінка:** 3 год

---

### Задача 3.2: Створити темну тему (Dark Mode)

**Кроки:**
1. Додати CSS змінні для темної теми:
```css
@media (prefers-color-scheme: dark) {
    :root {
        --color-bg: #1a1a2e;
        --color-surface: #2d2d44;
        --color-text: #e9ecef;
        --color-text-muted: #adb5bd;
        --color-border: #444;
    }
}
```
2. Додати перемикач теми в navbar
3. Зберегти вибір в localStorage
4. Оновити всі компоненти для підтримки темної теми

**Файли:** `styles.css`, `navbar.html`, JS для перемикача  
**Оцінка:** 4 год

---

### Задача 3.3: Покращити сторінку помилок

**Поточна:** Базова сторінка з `error.webp`.

**Кроки:**
1. Створити брендовану сторінку 404:
```html
<div class="error-page">
    <h1 class="display-1">404</h1>
    <p class="lead">На жаль, сторінку не знайдено</p>
    <p>Можливо, вона була видалена або переміщена</p>
    <a href="/" class="btn btn-primary">Повернутися на головну</a>
</div>
```
2. Додати анімовану ілюстрацію (SVG)
3. Стилізувати для темної теми

**Файли:** `src/main/resources/templates/error/404.html` + CSS  
**Оцінка:** 2 год

---

### Задача 3.4: Оптимізувати фавіконки

**Поточна:** Тільки `favicon.ico`.

**Кроки:**
1. Створити фавіконки різних розмірів:
```
static/assets/
├── favicon.ico
├── favicon-16x16.png
├── favicon-32x32.png
├── apple-touch-icon.png (180x180)
├── android-chrome-192x192.png
└── android-chrome-512x512.png
```
2. Оновити `<head>` в `common.html`:
```html
<link rel="icon" type="image/png" sizes="32x32" href="/assets/favicon-32x32.png">
<link rel="icon" type="image/png" sizes="16x16" href="/assets/favicon-16x16.png">
<link rel="apple-touch-icon" sizes="180x180" href="/assets/apple-touch-icon.png">
<link rel="manifest" href="/assets/site.webmanifest">
```

**Файли:** Нові зображення + `common.html`  
**Оцінка:** 1.5 год

---

### Задача 3.5: Оптимізувати Font Awesome

**Поточна:** Повна бібліотека (~200KB).

**Кроки:**
1. Створити кастомний набір іконок (SVG sprite)
2. Або використати Font Awesome Subsetting
3. Або замінити на Bootstrap Icons (менша бібліотека)

**Очікуваний ефект:** -80% розміру іконок  
**Оцінка:** 2 год

---

### Задача 3.6: Покращити Footer

**Поточний:** 4-колонковий MDB-стиль.

**Кроки:**
1. Оновити дизайн footer:
```css
.footer {
    background: var(--color-dark);
    color: var(--color-text-light);
    padding: 3rem 0 1rem;
}
.footer h5 {
    color: var(--color-primary-light);
    font-family: 'Montserrat', sans-serif;
}
.footer a {
    color: var(--color-text-light);
    transition: color 0.2s ease;
}
.footer a:hover {
    color: var(--color-primary-light);
}
```
2. Додати social media іконки
3. Додати копірайт та посилання на політику конфіденційності

**Файли:** `common.html` (footer фрагмент) + CSS  
**Оцінка:** 2 год

---

### ✅ Критерії завершення Фази 3

- [ ] Анімації працюють плавно (60fps)
- [ ] Темна тема повністю функціональна
- [ ] Сторінка 404 брендована
- [ ] Фавіконки відображаються на всіх пристроях
- [ ] Font Awesome оптимізовано
- [ ] Footer оновлено
- [ ] Lighthouse: Performance ≥ 90, Accessibility ≥ 95, SEO ≥ 95

---

## 📊 Зведена таблиця задач

| # | Задача | Фаза | Оцінка | Пріоритет |
|---|--------|------|--------|-----------|
| 1.1 | Виправити HTML `tracksall.html` | 1 | 30 хв | 🔴 |
| 1.2 | Закрити незакриті теги | 1 | 20 хв | 🔴 |
| 1.3 | Переписати SCSS → CSS | 1 | 1 год | 🔴 |
| 1.4 | Виправити контраст кольорів | 1 | 45 хв | 🔴 |
| 2.1 | Уніфікувати відступи | 2 | 2 год | 🟡 |
| 2.2 | Винести inline стилі | 2 | 3 год | 🟡 |
| 2.3 | Прибрати закоментований код | 2 | 2 год | 🟡 |
| 2.4 | Lazy loading зображень | 2 | 1.5 год | 🟡 |
| 2.5 | CSS для банерів | 2 | 1.5 год | 🟡 |
| 2.6 | Стилізувати дрібниці (RSS, плеєр) | 2 | 1 год | 🟡 |
| 2.7 | Замінити зовнішні зображення | 2 | 1.5 год | 🟡 |
| 3.1 | Анімації та hover-ефекти | 3 | 3 год | 🟢 |
| 3.2 | Темна тема | 3 | 4 год | 🟢 |
| 3.3 | Покращити сторінку помилок | 3 | 2 год | 🟢 |
| 3.4 | Оптимізувати фавіконки | 3 | 1.5 год | 🟢 |
| 3.5 | Оптимізувати Font Awesome | 3 | 2 год | 🟢 |
| 3.6 | Покращити Footer | 3 | 2 год | 🟢 |
|   | **РАЗОМ** |   | **~29 год** |   |

---

## 🏁 Загальні критерії завершення проекту

- [ ] Всі задачі Фази 1 виконано
- [ ] Всі задачі Фази 2 виконано
- [ ] Всі задачі Фази 3 виконано
- [ ] Lighthouse Performance ≥ 90
- [ ] Lighthouse Accessibility ≥ 95
- [ ] Lighthouse Best Practices ≥ 95
- [ ] Lighthouse SEO ≥ 95
- [ ] Жодного критичного багу
- [ ] Крос-браузерне тестування пройдено (Chrome, Firefox, Safari, Edge)
- [ ] Мобільне тестування пройдено

---

## 📝 Примітки

- Оцінки часу включають розробку, тестування та код-рев'ю
- Фаза 3 може бути виконана паралельно з Фазою 2 для окремих задач
- Для задач 3.2 (темна тема) та 3.1 (анімації) рекомендується створити окрему гілку
- Всі зміни мають бути покриті тестами згідно `TEST_PLAN_TINYMCE.md`

---

*Документ створено на основі аналізу в `DESIGN_ANALІЗ.md`*
