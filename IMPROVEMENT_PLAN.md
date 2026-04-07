# 🚀 План покращення якості проекту RFA

**Дата створення:** 2026-04-07  
**Останнє оновлення:** 2026-04-07  
**Статус:** В роботі  
**Всього задач:** 20  
**Прогрес:** 8/20 (40%)

---

## 📊 Статистика проекту

| Метрика | Значення |
|---------|----------|
| Java файлів | 295 |
| Рядків коду | ~24,760 |
| Шаблонів Thymeleaf | 139 |
| CSS/JS файлів | 40+ |
| Активних профілів | 5 (current, default, defaultrem, dell, mapi) |
| Комітів на `ai` | 18+ |

---

## ✅ Виконані задачі

| # | Задача | Фаза | Дата | Коміт |
|---|--------|------|------|-------|
| ✅ 1.3 | `@CrossOrigin` обмеження | HIGH | 2026-04-07 | `bc28cd45` |
| ✅ 2.8 | `org.json` оновлення (2009→2024) | MEDIUM | 2026-04-07 | `bc28cd45` |
| ✅ 2.5 | Security Headers (CSP, HSTS, XSS) | MEDIUM | 2026-04-07 | `bc28cd45` |
| ✅ #2 | **permitAll() на /admin/**, /user/**, /creater/** | HIGH | 2026-04-07 | `20bed808` |
| ✅ 1.2 | `th:utext` XSS санітизація (OWASP Sanitizer) | HIGH | 2026-04-07 | `7728586f` |
| ✅ — | TinyMCE 7 self-hosted інтеграція | HIGH | 2026-04-07 | `10a270e2` |
| ✅ — | Завантаження треку (Dropzone + форма) | HIGH | 2026-04-07 | `ba357f8c` |
| ✅ — | Сторінки помилок (404/403/500) | MEDIUM | 2026-04-07 | `2ee58700` |

---

## 🔴 Фаза 1 — Високий пріоритет (Безпека + Стабільність)

### 📋 1.4 ProcessBuilder Command Injection

**Проблема:** 18 викликів `ProcessBuilder` з конкатенацією рядків (docker, bash).  
**Ризик:** HIGH — command injection якщо параметри контрольовані користувачем.  
**Зусилля:** 3-5 днів  
**Файли:** `ServerRunnerService.java` (10), `RPCRESTController.java`, `STTBackServerService.java`, `AdminPrepare.java`

**План:**
- [ ] 1.4.1 Аудит кожного ProcessBuilder — чи є user-controlled input
- [ ] 1.4.2 Додати валідацію/escape параметрів (UUID whitelist)
- [ ] 1.4.3 Використовувати `ProcessBuilder(List<String>)` замість `bash -c`
- [ ] 1.4.4 Додати timeout для процесів
- [ ] 1.4.5 Логування команд без sensitive даних

**Критерії успіху:** Жоден user input не потрапляє в shell без валідації.

---

### 📋 1.1 FetchType.EAGER → LAZY

**Проблема:** 74 EAGER завантаження викликають N+1 запити та Cartesian product.  
**Ризик:** HIGH — серйозна деградація продуктивності при рості даних.  
**Зусилля:** 1-2 тижні  
**Файли:** `Clientdetail.java` (9 колекцій), `AccPosting.java` (10), `AccTransaction.java` (4)

**План:**
- [ ] 1.1.1 Змінити `FetchType.EAGER` → `FetchType.LAZY` в `Clientdetail.java`
- [ ] 1.1.2 Змінити в `AccPosting.java`, `AccTransaction.java` та інших
- [ ] 1.1.3 Додати `JOIN FETCH` або `@EntityGraph` в репозиторії для критичних запитів
- [ ] 1.1.4 Перевірити що жоден шаблон не падає з LazyInitializationException
- [ ] 1.1.5 Прибрати `enable_lazy_load_no_trans=true` з конфігурації

**Критерії успіху:** Кількість SQL запитів на сторінку зменшена на 50%+.

---

### 📋 1.5 Тестове покриття

**Проблема:** 0% — єдиний тест закоментований/порожній.  
**Ризик:** HIGH — регресійні баги, неможливість безпечного рефакторингу.  
**Зусилля:** 3-4 тижні (поетапно)  
**Файли:** `SecurityApplicationTests.java`

**План (поетапно):**
- [ ] 1.5.1 Полагодити/розкоментувати існуючий тест
- [ ] 1.5.2 Додати MockMvc конфігурацію
- [ ] 1.5.3 Unit тести для сервісних шарів (почати з критичних: Security, ClientService)
- [ ] 1.5.4 Integration тести для контролерів (почати з auth)
- [ ] 1.5.5 Test containers для PostgreSQL
- [ ] 1.5.6 Досягти 30% coverage

**Критерії успіху:** 30%+ coverage, CI pipeline проходить.

---

## 🟡 Фаза 2 — Середній пріоритет

### 📋 2.1 TODO/FIXME розбір (37 instances)
**Зусилля:** 2-3 дні | **Файли:** 15+ файлів

- [ ] 2.1.1 Класифікувати TODO за типом (баг, фіча, рефакторинг)
- [ ] 2.1.2 Створити GitHub Issues для кожного
- [ ] 2.1.3 Вирішити критичні (ServerRunnerService, StationService)

---

### 📋 2.2 System.out → SLF4J (22 println + 51 printStackTrace)
**Зусилля:** 2-3 дні | **Файли:** 30+ файлів

> ⏸️ **ЗАЛИШАЄМО НА КІНЕЦЬ** за рішенням користувача.

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

### 📋 2.7 @Transactional аудит
**Зусилля:** 2-3 дні

- [ ] 2.7.1 Знайти всі методи що змінюють дані без `@Transactional`
- [ ] 2.7.2 Додати анотації
- [ ] 2.7.3 Перевірити propagation level

---

### 📋 2.6 Оптимізація зображень
**Зусилля:** 1 день

- [ ] 2.6.1 Конвертувати в WebP
- [ ] 2.6.2 Додати `loading="lazy"` до всіх `<img>`
- [ ] 2.6.3 Додати `srcset` для responsive

---

### 📋 2.9 ddl-auto=update → Flyway/Liquibase
**Зусилля:** 2-3 дні

- [ ] 2.9.1 Додати Flyway dependency
- [ ] 2.9.2 Створити baseline міграцію з поточної схеми
- [ ] 2.9.3 Змінити `ddl-auto=update` → `validate` в production

---

## 🟢 Фаза 3 — Низький пріоритет (Code Style)

### 📋 3.1 God-класи рефакторинг (12 файлів >300 рядків)
**Зусилля:** 3-4 тижні

- [ ] 3.1.1 `PodcastService.java` (827 рядків)
- [ ] 3.1.2 `ServerRunnerService.java` (600)
- [ ] 3.1.3 `ClientHomeStationController.java` (575)
- [ ] 3.1.4 `RSSXMLService.java` (541)
- [ ] 3.1.5 `StoreSiteController.java` (500)
- [ ] 3.1.6 `NewsHome.java` (457)
- [ ] 3.1.7 `UserLoginController.java` (451)
- [ ] 3.1.8 `PodcastController.java` (410)
- [ ] 3.1.9 `PostController.java` (341)
- [ ] 3.1.10 `STTHome.java` (325)
- [ ] 3.1.11 `ClientService.java` (317)
- [ ] 3.1.12 `StationService.java` (316)

---

### 📋 3.2 Видалення закоментованого коду (345 рядків)
**Зусилля:** 2-3 години

- [ ] 3.2.1 Автоматичний пошук та видалення
- [ ] 3.2.2 Зберегти важливі коментарі як TODO

---

### 📋 3.3 Перейменування пакетів (12 директорій)
**Зусилля:** 1-2 дні

- [ ] 3.3.1 `repositore` → `repository` (5 пакетів)
- [ ] 3.3.2 `sevice` → `service`
- [ ] 3.3.3 `rtansactiom` → `transaction`
- [ ] 3.3.4 `referens` → `reference`
- [ ] 3.3.5 `messanger` → `messenger`
- [ ] 3.3.6 `Reposirore` → `Repository`

---

### 📋 3.4 Naming conventions
**Зусилля:** 1-2 дні

- [ ] 3.4.1 PascalCase методи → camelCase (`GetListStationByUser` → `getListStationByUser`)
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

### 📋 3.7 Consolidate upload controllers
**Зусилля:** 3-5 днів

- [ ] 3.7.1 Об'єднати 7 drop-file контролерів в 1 універсальний

---

### 📋 3.8 Accessibility
**Зусилля:** 2-3 дні

- [ ] 3.8.1 Додати `alt` до всіх `<img>`
- [ ] 3.8.2 Додати ARIA labels
- [ ] 3.8.3 Skip navigation link
- [ ] 3.8.4 Focus management

---

### 📋 3.9 Hardcoded Passwords → Environment Variables
**Зусилля:** 2-3 дні

> ⏸️ **ЗАЛИШАЄМО НА КІНЕЦЬ** за рішенням користувача.

- [ ] 3.9.1 Винести `spring.datasource.password` в ENV vars
- [ ] 3.9.2 Винести `rabbitmq.password` в ENV vars
- [ ] 3.9.3 Винести `icecast.*password` в ENV vars
- [ ] 3.9.4 Ротувати всі паролі після деплою

---

## 📈 Прогрес

```
Фаза 1 (HIGH):    [███████░░░]  60%  (3/5 виконано, 2 залишилось)
Фаза 2 (MEDIUM):  [███░░░░░░░]  25%  (2/8 виконано, 6 залишилось)
Фаза 3 (LOW):     [░░░░░░░░░░]   0%  (0/9 виконано)
──────────────────────────────────────
Загалом:           [████░░░░░░]  40%  (8/20 виконано)
```

---

## 📅 Рекомендований порядок виконання

### Наступний спринт (Тиждень 1): Безпека та Продуктивність
1. **ProcessBuilder валідація** (1.4) — 3-5 днів, HIGH ризик
2. **@Transactional аудит** (2.7) — 2-3 дні, цілісність даних

### Тиждень 2: Продуктивність
3. **FetchType.EAGER → LAZY** (1.1) — 1-2 тижні, -60% запитів

### Тиждень 3: Інфраструктура
4. **Actuator** (2.4) — 1 день
5. **Flyway** (2.9) — 2-3 дні
6. **Response Caching** (2.3) — 1-2 дні

### Тиждень 4-5: Якість коду
7. **TODO/FIXME розбір** (2.1) — 2-3 дні
8. **Видалити закоментований код** (3.2) — 2-3 год
9. **Перейменування пакетів** (3.3) — 1-2 дні
10. **CI/CD** (3.5) — 2-3 дні

### Тиждень 6-8: Тестування
11. **Unit тести** (1.5) — 3-4 тижні

### Тиждень 9+: Рефакторинг (Фаза 3)
12. **God-класи** (3.1)
13. **Upload controllers consolidate** (3.7)
14. **Dockerfile** (3.6)
15. **Accessibility** (3.8)
16. **Паролі → ENV** (3.9) — в кінці
17. **System.out → SLF4J** (2.2) — в кінці

---

## 🚀 Швидкі перемоги (можна зробити сьогодні)

| Задача | Зусилля | Вплив |
|--------|---------|-------|
| Видалити закоментований код (3.2) | 2-3 год | 🟢 Code readability |
| Actuator (2.4) | 1 день | 🟡 Monitoring |
| Оптимізація зображень (2.6) | 1 день | 🟡 Page load -30% |

---

*Документ створено 2026-04-07. Останнє оновлення: 2026-04-07*
