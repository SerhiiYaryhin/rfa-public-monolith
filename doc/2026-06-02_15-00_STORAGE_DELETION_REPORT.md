# Звіт: Реалізація видалення об'єктів сховища (Storage Objects)
**Дата:** 2 червня 2026 р.
**Статус:** Виконано Крок 1 та Крок 2

## 1. Аналіз моделі даних та залежностей (Завершено)

Об'єкти сховища (`Store`) інтегровані в багато модулів системи. Виявлено всі ключові сутності, що використовують `Store`.

### Основні сутності, що використовують `Store`:

| Сутність | Поле посилання | Тип зв'язку | Коментар |
| :--- | :--- | :--- | :--- |
| **Track** | `storeitem` / `storeuuid` | OneToOne / String | Аудіофайл треку |
| **Post** | `coverstoreuuid` | String | Головна ілюстрація посту |
| **AuthorColumn** | `avatarUuid` | String | Фото автора колонки |
| **AuthorArticle** | `coverUuid` | String | Обкладинка статті |
| **PodcastChannel**| `imagechanelstore` | OneToOne | Обкладинка подкасту |
| **PodcastItem** | `enclosurestore` | ManyToOne | Аудіофайл епізоду |
| **PodcastItem** | `imagestoreitem` | ManyToOne | Обкладинка епізоду |
| **Album** | `storeuuidalbumcover`| String | Обкладинка альбому |
| **Documents** | `store` / `storeuuid` | OneToOne / String | Скани документів |
| **Clientdetail** | `profilephoto` | ManyToOne | Фото профілю користувача |
| **News** | `storespeach` | OneToOne | Згенерований голос для новини |

## 2. Виконана робота (Крок 1 та 2)

### 2.1. Централізація пошуку залежностей
Оновлено `StoreDependencyService`. Тепер він враховує всі виявлені сутності. Додано підтримку нових репозиторіїв:
- `AuthorArticleRepository`
- `AuthorColumnRepository`
- `AlbumRepository`
- `DocumentRepository`
- `ClientDetailRepository`
- `NewsRepositore`

Для цього в репозиторії були додані відповідні методи пошуку (`findBy...`).

### 2.2. Створення сервісу очищення (StoreCleanupService)
Створено новий сервіс `StoreCleanupService`, який став єдиною точкою входу для операцій з видалення та від'єднання файлів:
- `detachAll(Store store)`: Рекурсивно (по всіх сутностях) обнуляє посилання на вказаний об'єкт сховища.
- `performSafeDelete(Store store, boolean onlyDetach)`: Виконує повний цикл очищення та (за бажанням) фізичне видалення файла.
- `canUserDelete(Store store, Users user)`: Перевіряє права доступу на видалення з урахуванням ролей (`ROLE_ADMIN`, `ROLE_EDITOR`, `ROLE_CREATER`, `ROLE_AUTHOR`).

### 2.3. Рефакторинг існуючого коду
- **CreaterStoreController**: Повністю переписано методи видалення (`deleteStoreItem`, `confirmDeleteStoreItem`). Тепер вони використовують `StoreCleanupService` та перевіряють права доступу.
- **PodcastService**: Метод `DetachStoreFromPodcasts` делегує роботу новому `StoreCleanupService`.

## 3. Наступні кроки

1. **Frontend**: Оновлення шаблону `confirm_delete.html` для коректного відображення нових типів залежностей (статті, колонки, новини).
2. **UI**: Додати модальне вікно підтвердження безпосередньо в список файлів (`mainstore.html`).
3. **Testing**: Написання інтеграційних тестів для перевірки безпечного видалення.
4. **Cleanup Task**: Реалізація `Scheduled` задачі для видалення файлів, що не мають записів у БД.
