# ParseThroughAPI — Java-парсер MAL → PostgreSQL

**Коротко:** парсер собирает данные MyAnimeList через Jikan (и прямой парсинг), нормализует и сохраняет в PostgreSQL с помощью Hibernate (JPA) + HikariCP. Проект умеет: массово скачивать топ-аниме, подтягивать списки пользователей, восстанавливать целостность данных, экспортировать подготовленные датасеты в Parquet для ML (с анонимизацией ID).

---

## Основные возможности

* Сбор метаданных аниме (top/anime) и их сохранение в БД.
* Сбор профилей пользователей и их списков (animelist), сохранение `user_stat` и `user_anime_stat`.
* Маппинг справочников: `producer`, `genre`, `demographic` с реюзом сущностей (find-or-create).
* Инструменты восстановления целостности данных (resync) — сравнение `user_stat.total_entries` с реальным числом записей и ресинк при рассогласовании.
* Экспорт данных в Parquet (Avro schema) для дальнейшего использования в ML-пайплайнах; возможность анонимизации ID.
* Устойчивость: retry, rate-limiting, backoff, обработка GZIP, защита от «tainted hosts» (captcha), аккуратное управление OkHttp клиентом.

---

## Быстрый обзор структуры проекта

```
src/main/java/
├─ AnimeParsing/        # парсинг аниме (Jikan)
├─ UserParsing/         # парсинг пользователей и списков (MAL)
├─ Data/                # JPA-сущности (Anime, Users, UserStat, ...)
├─ Mapper/              # Mapper'ы DTO -> JPA entity
├─ Scripts/             # утилиты: FetchAndPersist, DataIntegrityRestorer, UserResyncService, DataOutputToFile
├─ Utils/               # утилиты: OkHttpClientManager, DateTime, SimpleDataExtract, SchemaGenerator
└─ (несколько mains)    # запусковые классы: FetchingListOfAnime, FetchingUserAndStats, RestoringDataIntegrity, SaveDataFromSQLToParquetFile
```

---

## Схема БД (сокращённо)

**Основные таблицы:**

* `anime` — метаданные (mal_id PK, title, url, score, synopsis, season, year и т.д.)
* `producer`, `genre`, `demographic` — справочники (mal_id PK)
* `users` — профиль пользователя (mal_id PK, username, joined, last_online и т.д.)
* `user_stat` — агрегированные статистики (one-to-one с users)
* `user_anime_stat` — записи о взаимодействии пользователя с аниме (composite PK: user_id, anime_id)
* join-таблицы M:N: `anime_producer`, `anime_licensor`, `anime_studio`, `anime_genre`, `anime_theme`, `anime_demographic`

Полное описание колонок и типов — в JPA-сущностях `Data.*` (см. `Data.Anime`, `Data.Producer`, `Data.Genre`, `Data.Demographic`, `Data.Users`, `Data.UserStat`, `Data.UserAnimeStat`).

---

## Конфигурация — что важно знать

### Переменные окружения / system properties

Проект использует комбинацию `System.getProperty(...)` и `System.getenv(...)`.
Основные переменные:

* `DB_URL` / `hibernate.hikari.dataSource.url` — JDBC URL (по умолчанию `jdbc:postgresql://localhost:5432/anime_db` в examples)
* `DB_USER` / `hibernate.hikari.dataSource.user` — пользователь БД
* `DB_PASS` / `hibernate.hikari.dataSource.password` — пароль
* `JIKAN_BASE` / `jikan.base` — базовый URL Jikan API (по умолчанию `https://api.jikan.moe/v4`)
* `MAL_HOST` / `mal.base` — базовый URL MyAnimeList (по умолчанию `https://myanimelist.net`)

Параметры для запуска скриптов/утилит (примерные названия переменных):

* `NUMBER_OF_PAGE_FROM_ANIME_TOP` — число страниц топ-аниме (если не указано, используется `countPages()`).
* `NUMBER_OF_USER_TO_FETCH` — сколько пользователей подтянем в `FetchingUserAndStats`.
* `MINIMUM_NUMBER_OF_ANIME_IN_USER_LISTS` — фильтр по минимальному количеству записей в списке пользователя.
* `MINIMUM_NUMBER_OF_COMPLETED_ANIME_IN_USER_LISTS` — минимальное число завершённых аниме.
* `BATCH_SIZE`, `THRESHOLD_PERCENTAGE` — для `DataIntegrityRestorer`.
* `SHOW_SQL` — включить печать SQL при экспорте в Parquet.
* Параметры экспорта: `ANIME_COLUMNS`, `USER_ANIME_COLUMNS`, `ANIME_FILTERS`, `ANIME_EVALUATION_FILTERS`.

---

## Сборка и запуск

Проект совместим со стандартным Maven-процессом (или сборкой в IDE).

Сборка (Maven):

```bash
mvn clean package
```

Запуск из IDE: просто запустите нужный `main` класс.

Примеры запусков (из JAR или через maven-exec):

Запуск скачивания топ-аниме (в `FetchingListOfAnime`):

```bash
# с помощью maven-exec
mvn exec:java -Dexec.mainClass="FetchingListOfAnime"

# или, если собрали fat-jar (например target/parsethroughapi.jar):
java -cp target/parsethroughapi.jar FetchingListOfAnime
```

Запуск скачивания пользователей и их списков:

```bash
mvn exec:java -Dexec.mainClass="FetchingUserAndStats"
```

Запуск процесса восстановления целостности:

```bash
mvn exec:java -Dexec.mainClass="RestoringDataIntegrity"
```

Экспорт данных в Parquet (создаёт `out/anime.parquet` и `out/evaluations.parquet`):

```bash
mvn exec:java -Dexec.mainClass="SaveDataFromSQLToParquetFile"
```

Генерация схемы JPA (полезно для локальной отладки):

```bash
mvn exec:java -Dexec.mainClass="Utils.SchemaGenerator"
```
---

## Важные детали реализации и рекомендации

### 1) Производительность и устойчивость

* **Пул потоков и батчи:** при массовом импортe используются fixed thread pools (по ~7 потоков) и batch-коммиты/flush для уменьшения памяти и повышения скорости вставки (`hibernate.jdbc.batch_size` = 100).
* **Retry / backoff:** при сетевых ошибках реализованы многократные попытки с экспоненциальным бэкоффом.
* **Rate-limiter для MAL:** `SimpleRateLimiter` и `MAL_RATE_LIMITER` защищают от частых запросов в MAL (предотвращают 429/блокировки).

### 2) Защита от `captcha` и `tainted hosts`

Если при парсинге ответ содержит признаки «human verification», хост помечается (tainted) и на него накладывается таймаут — библиотека будет ждать cooldown, чтобы не давить дальше.

### 3) Id-регистрация и анонимизация

При экспорте в Parquet есть возможность анонимизировать ID (`IdAnonymizer`) — хорошо для публикации датасетов.

### 4) Парсинг дат/времён

`Utils.DateTime` умеет парсить ISO-форматы и epoch seconds; используется при переводе timestamp полей пользователей.

### 5) OkHttpClient lifecycle

`OkHttpClientManager` создаёт отдельный `Dispatcher` и `ConnectionPool`, аккуратно закрывает старые ресурсы при пересоздании клиента — важно при долгих задачах.

---

## Экспорт в Parquet — примечания

* Экспорт выполняется через `SimpleDataExtract.exportQueryToParquet(...)` с автогенерацией Avro-схемы по мета-информации ResultSet.
* Можно передать набор колонок для анонимизации (например `user_id`).
* По умолчанию используется SNAPPY-сжатие.

---

## Типичные проблемы и их решения

* **Captcha / блокировки от MAL:** проект пытается обнаруживать и «тащить» cooldown, но если проблем много — уменьшите параллелизм, увеличьте задержки (`MAL_RATE_LIMITER`), или используйте прокси.
* **HTTP 429 / 500:** уже есть обработка; при частых 429 — уменьшите скорость/увеличьте jitter и задержки.
* **Ошибка подключения к БД / Hikari:** проверьте `DB_URL`, `DB_USER`, `DB_PASS`. Логи Hikari показывают линию причины (timeout, max pool exhausted).
* **Memory / GC:** уменьшите количество одновременно обрабатываемых записей в батче или увеличьте heap при запуске JVM.
* **JSON -> JPA несовпадение:** убедитесь, что поля DTO корректно парсятся (SnakeCase для Jikan). Для проблем с маппингом — включите логи маппера/Jackson.

---


