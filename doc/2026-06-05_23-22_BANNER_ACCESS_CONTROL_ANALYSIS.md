# Аналіз розділення прав користувачів для роботи з банерами

**Дата та час аналізу:** 05.06.2026 23:22 (локальний час: 2026-06-05T23:22:08+03:00)  
**Об'єкт аналізу:** Політика безпеки, ролі користувачів та механізми авторизації у підсистемі банерів.

---

## 1. Ролі користувачів в системі RFA
Відповідно до файлу конфігурації безпеки [SecurityConfig.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/security/config/SecurityConfig.java), у системі існують такі основні ролі:
* `ADMIN` (Адміністратор) — повний доступ до всіх ресурсів системи.
* `MODERATOR` (Модератор) — перегляд та керування контентом.
* `CREATER` (Творець контенту / Автор) — керування власним контентом.
* `USER` (Звичайний зареєстрований користувач).
* Інші спеціалізовані ролі (`AUTHOR`, `ACCCHEAF`, `EDITOR`, `NEWSTORADIO`).

Для роботи з банерами критично важливим є зв'язок сутності користувача `Users` із сутністю [Clientdetail](file:///home/coder/rfa/src/main/java/media/toloka/rfa/radio/model/Clientdetail.java) (дані клієнта-рекламодавця). Кожен користувач, який створює банери, повинен мати пов'язаний запис `Clientdetail`.

---

## 2. Аналіз поточного стану безпеки та розділення прав

Наразі у підсистемі банерів виявлено кілька критичних архітектурних проблем безпеки та розмежування доступу.

### 2.1. Проблема доступу гостей (Guest/Anonymous Access Bug)
* **Поточна логіка:**  
  Ендпоінти завантаження банерів на сторінки клієнтів (`POST /api/2.0/banners/load`) та трекінгу статистики (`POST /api/2.0/banners/{uuid}/view` / `transition`) знаходяться у REST-контролері `BannerRestController` під мапінгом `/api/2.0/banners/**`.  
  У [SecurityConfig.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/security/config/SecurityConfig.java) дозволено публічний доступ тільки до `/api/1.0/**`. Шлях `/api/2.0/**` підпадає під загальне правило `.anyRequest().authenticated()`.
* **Наслідки:**  
  Неавторизовані відвідувачі сайту (гості) отримуватимуть помилки `401 Unauthorized` при спробі завантажити сторінки з банерами, оскільки їхні браузери не зможуть виконати фонові запити до API банерів.
* **Рішення:**  
  Додати `/api/2.0/banners/**` до переліку публічних ендпоінтів (`.permitAll()`) у конфігурації Spring Security.

### 2.2. Відсутність ізоляції клієнтів (Client Isolation Vulnerability)
* **Поточна логіка:**  
  Контролери перевіряють лише наявність авторизованого користувача та пов'язаного з ним об'єкта `Clientdetail` (наприклад, `clientService.GetClientDetailByUser(currentUser) == null`). Проте жоден метод редагування, оновлення медіа чи видалення не перевіряє, **кому саме** належить цей банер.
* **Наслідки:**  
  Будь-який авторизований користувач із профілем рекламодавця може:
  - Редагувати чужі банери через `POST /banners/save` (передавши чужий `uuid`).
  - Видаляти чужі банери через `POST /banners/{uuid}/delete`.
  - Замінювати медіафайли на чужих банерах через `PUT /api/2.0/banners/{bannerUuid}/media/{mediaUuid}`.
* **Рішення:**  
  Впровадити перевірку володіння об'єктом. Клієнт може змінювати банер тільки якщо `banner.getClientdetail().getUuid().equals(currentClientDetail.getUuid())`.

### 2.3. Несанкціоноване самосхвалення банерів (Self-Approval Vulnerability)
* **Поточна логіка:**  
  Ендпоінт `POST /banners/{uuid}/toggle-approve` у [BannersController.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/banner/controller/BannersController.java) дозволяє змінювати статус модерації банера (`approve = true/false`). Цей метод перевіряє лише те, що користувач авторизований як клієнт.
* **Наслідки:**  
  Будь-який рекламодавець може самостійно схвалити свій банер для публікації, повністю оминаючи процес адміністративної перевірки модератором чи адміністратором системи.
* **Рішення:**  
  Обмежити доступ до методу `toggleApprove` та відповідної кнопки в інтерфейсі тільки для користувачів із ролями `ADMIN` або `MODERATOR` (наприклад, за допомогою `@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")`).

---

## 3. Матриця доступу до функцій керування банерами

| Функція / Дія | Гість (Guest) | Рекламодавець (Client) | Адмін / Модератор | Поточний стан (Проблема) | Безпечний (Очікуваний) стан |
| :--- | :---: | :---: | :---: | :--- | :--- |
| **Відображення банерів на сайті** | ✅ | ✅ | ✅ | ❌ Заблоковано для гостей (401) | ✅ Дозволено для всіх (`permitAll`) |
| **Реєстрація переглядів/кліків** | ✅ | ✅ | ✅ | ❌ Заблоковано для гостей (401) | ✅ Дозволено для всіх (`permitAll`) |
| **Перегляд списку банерів (Admin UI)** | ❌ | ❌ | ✅ | ❌ Дозволено будь-якому автентифікованому | ✅ Дозволено тільки Admin / Moderator |
| **Створення нового банера** | ❌ | ✅ | ✅ | ✅ Працює коректно | ✅ Дозволено клієнтам та адмінам |
| **Редагування банера** | ❌ | ✅ (Тільки свої) | ✅ (Всі) | ❌ Дозволено будь-якому клієнту для будь-якого банера | ✅ Перевірка права власності або роль Admin |
| **Завантаження медіа для банера** | ❌ | ✅ (Тільки свої) | ✅ (Всі) | ❌ Дозволено будь-якому клієнту для будь-якого банера | ✅ Перевірка права власності або роль Admin |
| **Видалення банера** | ❌ | ✅ (Тільки свои) | ✅ (Всі) | ❌ Дозволено будь-якому клієнту для будь-якого банера | ✅ Перевірка права власності або роль Admin |
| **Схвалення публікації (Approve)** | ❌ | ❌ | ✅ | ❌ Клієнти можуть самі схвалювати свої банери | ✅ Дозволено тільки Admin / Moderator |

---

## 4. Рекомендації щодо безпечного налаштування прав

### 4.1. Зміни в конфігурації Spring Security
У [SecurityConfig.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/security/config/SecurityConfig.java) необхідно розділити доступ:
```java
// Дозволити публічний доступ до API завантаження та статистики
.requestMatchers(
    "/api/2.0/banners", "/api/2.0/banners/random", 
    "/api/2.0/banners/*/view", "/api/2.0/banners/*/transition", 
    "/api/2.0/banners/load"
).permitAll()

// Дозволити перегляд списку банерів в панелі керування лише адміністраторам та модераторам
.requestMatchers("/banners").hasAnyRole("ADMIN", "MODERATOR")
.requestMatchers("/banners/new").hasAnyRole("USER", "CREATER", "ADMIN")
.requestMatchers("/banners/*/edit").hasAnyRole("USER", "CREATER", "ADMIN")
```

### 4.2. Перевірка власності банера в контролерах
Необхідно впровадити допоміжний метод у сервіс або контролер для перевірки прав:
```java
private boolean canUserModifyBanner(Banner banner, Clientdetail cd, Users user) {
    if (user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"))) {
        return true; // Адміністратор може редагувати все
    }
    // Клієнт може редагувати тільки свій банер
    return banner.getClientdetail() != null && cd != null && 
           banner.getClientdetail().getUuid().equals(cd.getUuid());
}
```
Та використовувати його в методах `save`, `delete`, `editForm` та `clearBannerMedia` у [BannersController.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/banner/controller/BannersController.java).

### 4.3. Обмеження схвалення банерів
Для методу `toggleApprove` додати анотацію безпеки:
```java
@PostMapping("/{uuid}/toggle-approve")
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public String toggleApprove(@PathVariable String uuid) { ... }
```
Це гарантуватиме, що клієнти не зможуть самостійно активувати рекламу без фінансової перевірки чи цензурування модератором.
