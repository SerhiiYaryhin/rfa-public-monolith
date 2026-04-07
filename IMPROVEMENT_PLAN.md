# 🚀 План покращення якості проекту RFA

**Дата створення:** 2026-04-07  
**Статус:** В роботі  
**Всього задач:** 20  
**Прогрес:** 0/20

---

## 📊 Статистика проекту

| Метрика | Значення |
|---------|----------|
| Java файлів | 291 |
| Рядків коду | ~24,500 |
| Шаблонів Thymeleaf | 104 |
| CSS/JS файлів | 40+ |
| Активних профілів | 5 (current, default, defaultrem, dell, mapi) |

---

## 🔴 Фаза 1 — Високий пріоритет (Безпека + Стабільність)

### 📋 1.1 FetchType.EAGER → LAZY

**Проблема:** 74 EAGER завантаження викликають N+1 запити та Cartesian product.  
**Ризик:** HIGH — серйозна деградація продуктивності при рості даних.  
**Зусилля:** 3-5 днів  
**Файли:** `Clientdetail.java` (8 колекцій), `AccPosting.java` (10), `AccTransaction.java` (4)

**План:**
- [ ] 1.1.1 Змінити `FetchType.EAGER` → `FetchType.LAZY` в `Clientdetail.java`
- [ ] 1.1.2 Змінити в `AccPosting.java`, `AccTransaction.java`
- [ ] 1.1.3 Додати `JOIN FETCH` в репозиторії для критичних запитів
- [ ] 1.1.4 Перевірити що жоден шаблон не падає з LazyInitializationException
- [ ] 1.1.5 Покрити тестиами

**Критерії успіху:** Кількість SQL запитів на сторінку зменшена на 50%+.

---

### 📋 1.2 th:utext XSS аудит

**Проблема:** 49 використань `th:utext` з потенційно користувацьким контентом.  
**Ризик:** HIGH — збережений XSS через `post.postbody`, `podcast.description`, коментарі.  
**Зусилля:** 3-5 днів  
**Файли:** `admin.html` (21), `mediahub.html` (4), `postview.html`, `guest.html`

**План:**
- [ ] 1.2.1 Аудит кожного `th:utext` — чи може містити користувацький вміст
- [ ] 1.2.2 Додати OWASP Java HTML Sanitizer dependency
- [ ] 1.2.3 Створити `HtmlSanitizerService`
- [ ] 1.2.4 Замінити `th:utext` на `th:text` там де не потрібен HTML
- [ ] 1.2.5 Санітизувати HTML перед `th:utext` де потрібно
- [ ] 1.2.6 Додати Content-Security-Policy header

**Критерії успіху:** 0 `th:utext` без санітизації, CSP header присутній.

---

### 📋 1.3 @CrossOrigin обмеження

**Проблема:** 3 контролери з `@CrossOrigin` без обмежень по origin.  
**Ризик:** HIGH — будь-який сайт може робити запити з сесією користувача.  
**Зусилля:** 1-2 години  
**Файли:** `RestMobileApi.java`, `StoreItemController.java`, `StoreSiteController.java`

**План:**
- [ ] 1.3.1 Замінити `@CrossOrigin` → `@CrossOrigin(origins = {"https://rfa.toloka.media"})`
- [ ] 1.3.2 Додати allowed methods та max age

**Критерії успіху:** Тільки домені origins мають доступ до API.

---

### 📋 1.4 ProcessBuilder Command Injection

**Проблема:** 31 виклик shell-команд з конкатенацією рядків.  
**Ризик:** HIGH — command injection якщо параметри контрольовані користувачем.  
**Зусилля:** 3-5 днів  
**Файли:** `ServerRunnerService.java` (15), `RPCRESTController.java`, `STTBackServerService.java`

**План:**
- [ ] 1.4.1 Аудит кожного ProcessBuilder — чи є user-controlled input
- [ ] 1.4.2 Додати валідацію/escap параметрів
- [ ] 1.4.3 Використовувати `ProcessBuilder(List<String>)` замість `bash -c`
- [ ] 1.4.4 Додати timeout для процесів
- [ ] 1.4.5 Логування команд без sensitive даних

**Критерії успіху:** Жоден user input не потрапляє в shell без валідації.

---

### 📋 1.5 Тестове покриття

**Проблема:** 0% — єдиний тест закоментований.  
**Ризик:** HIGH — регресійні баги, неможливість безпечного рефакторингу.  
**Зусилля:** 4-8 тижнів (поетапно)  
**Файли:** `SecurityApplicationTests.java`

**План (поетапно):**
- [ ] 1.5.1 Розкоментувати/полагодити існуючий тест
- [ ] 1.5.2 Додати MockMvc конфігурацію
- [ ] 1.5.3 Unit тести для сервісних шарів (почати з критичних)
- [ ] 1.5.4 Integration тести для контролерів (почати з auth)
- [ ] 1.5.5 Test containers для PostgreSQL
- [ ] 1.5.6 Досягти 40% coverage

**Критерії успіху:** 40%+ coverage, CI pipeline проходить.

---

## 🟡 Фаза 2 — Середній пріоритет

### 📋 2.1 TODO/FIXME розбір (96 instances)
**Зусилля:** 2-4 спринти | **Файли:** 40+ файлів

- [ ] 2.1.1 Класифікувати TODO за типом (баг, фіча, рефакторинг)
- [ ] 2.1.2 Створити GitHub Issues для кожного
- [ ] 2.1.3 Вирішити критичні

---

### 📋 2.2 System.out → SLF4J (115 instances)
**Зусилля:** 2-3 дні | **Файли:** 30+ файлів

- [ ] 2.2.1 Замінити всі `System.out.println` → `logger.info/debug/warn/error`
- [ ] 2.2.2 Замінити `printStackTrace` → `logger.error("...", e)`
- [ ] 2.2.3 Налаштувати logback-spring.xml

---

### 📋 2.3 Response Caching
**Зусилля:** 1-2 дні

- [ ] 2.3.1 Додати Spring Cache + Caffeine
- [ ] 2.3.2 Кешувати довідники (enums, categories, stations)
- [ ] 2.3.3 Додати `@CacheEvict` при оновленні даних

---

### 📋 2.4 Actuator + Health Endpoints
**Зусилля:** 1 день

- [ ] 2.4.1 Додати `spring-boot-starter-actuator`
- [ ] 2.4.2 Налаштувати `/health`, `/info`, `/metrics`
- [ ] 2.4.3 Обмежити доступ до actuator

---

### 📋 2.5 Security Headers
**Зусилля:** 1-2 години

- [ ] 2.5.1 Додати Content-Security-Policy
- [ ] 2.5.2 Додати Strict-Transport-Security
- [ ] 2.5.3 Додати X-Content-Type-Options, X-Frame-Options, Referrer-Policy

---

### 📋 2.6 Оптимізація зображень
**Зусилля:** 1 день

- [ ] 2.6.1 Конвертувати в WebP
- [ ] 2.6.2 Додати `loading="lazy"` до всіх `<img>`
- [ ] 2.6.3 Додати `srcset` для responsive

---

### 📋 2.7 @Transactional аудит
**Зусилля:** 2-3 дні

- [ ] 2.7.1 Знайти всі методи що змінюють дані без `@Transactional`
- [ ] 2.7.2 Додати анотації
- [ ] 2.7.3 Перевірити propagation level

---

### 📋 2.8 org.json оновлення
**Зусилля:** 1-2 години

- [ ] 2.8.1 Оновити `org.json:json` з `20090211` → `20240303`
- [ ] 2.8.2 Перевірити сумісність API

---

## 🟢 Фаза 3 — Низький пріоритет

### 📋 3.1 God-класи рефакторинг (7 файлів >500 рядків)
**Зусилля:** 2-4 тижні

- [ ] 3.1.1 `PodcastService.java` (827 рядків)
- [ ] 3.1.2 `ServerRunnerService.java` (600)
- [ ] 3.1.3 `ClientHomeStationController.java` (575)
- [ ] 3.1.4 `RSSXMLService.java` (541)
- [ ] 3.1.5 `StoreSiteController.java` (496)
- [ ] 3.1.6 `NewsHome.java` (457)
- [ ] 3.1.7 `UserLoginController.java` (451)

---

### 📋 3.2 Видалення закоментованого коду (365 блоків)
**Зусилля:** 1-2 дні

- [ ] 3.2.1 Автоматичний пошук та видалення
- [ ] 3.2.2 Зберегти важливі коментарі як TODO

---

### 📋 3.3 Перейменування пакетів
**Зусилля:** 1-2 дні

- [ ] 3.3.1 `tetegrambot` → `telegrambot`
- [ ] 3.3.2 `repositore` → `repository`
- [ ] 3.3.3 `sevice` → `service`

---

### 📋 3.4 Naming conventions
**Зусилля:** 1-2 дні

- [ ] 3.4.1 PascalCase методи → camelCase
- [ ] 3.4.2 `Albumсover.java` (Cyrillic 'с') → Latin

---

### 📋 3.5 CI/CD Pipeline
**Зусилля:** 2-3 дні

- [ ] 3.5.1 Створити GitHub Actions workflow
- [ ] 3.5.2 Додати build, test, lint steps
- [ ] 3.5.3 Auto-deploy на staging

---

### 📋 3.6 Dockerfile для додатку
**Зусилля:** 1 день

- [ ] 3.6.1 Створити multi-stage Dockerfile
- [ ] 3.6.2 Додати docker-compose для dev

---

### 📋 3.7 Accessibility
**Зусилля:** 2-3 дні

- [ ] 3.7.1 Додати `alt` до всіх `<img>`
- [ ] 3.7.2 Додати ARIA labels
- [ ] 3.7.3 Skip navigation link
- [ ] 3.7.4 Focus management

---

## 📈 Прогрес

```
Фаза 1 (HIGH):    [░░░░░░░░░░]  0%  (0/5)
Фаза 2 (MEDIUM):  [░░░░░░░░░░]  0%  (0/8)
Фаза 3 (LOW):     [░░░░░░░░░░]  0%  (0/7)
──────────────────────────────────────
Загалом:           [░░░░░░░░░░]  0%  (0/20)
```

---

## 📅 Рекомендований порядок виконання

### Тиждень 1-2: Безпека
1. `@CrossOrigin` фікс (1.3) — 2 год, миттєвий виграш
2. `org.json` оновлення (2.8) — 1 год, закриття CVE
3. Security Headers (2.5) — 2 год
4. `th:utext` аудит (1.2) — 3-5 днів

### Тиждень 3-4: Продуктивність
5. FetchType.EAGER → LAZY (1.1) — 3-5 днів
6. Response Caching (2.3) — 1-2 дні
7. Actuator (2.4) — 1 день

### Тиждень 5-6: Якість коду
8. System.out → SLF4J (2.1) — 2-3 дні
9. @Transactional аудит (2.7) — 2-3 дні
10. ProcessBuilder валідація (1.4) — 3-5 днів

### Тиждень 7-8: Інфраструктура
11. Тести (1.5) — почати з критичних
12. CI/CD (3.5) — 2-3 дні
13. TODO/FIXME розбір (2.1) — поетапно

### Тиждень 9+: Рефакторинг
14. God-класи (3.1)
15. Перейменування пакетів (3.3)
16. Accessibility (3.7)

---

*Документ створено 2026-04-07. Останнє оновлення: 2026-04-07*
