# ParseThroughAPI — Java-парсер MAL → PostgreSQL

**Коротко:** парсер собирает данные с MyAnimeList (через Jikan / парсинг), сохраняет их в PostgreSQL (Hibernate JPA + HikariCP), выполняет валидацию и восстановление целостности, и помечает/отсеивает неполные записи на этапе экспорта для ML.

## Структура директории 
```
src/main/java/
├─ AnimeParsing/
│  ├─ Anime.java
│  ├─ AnimeTopResult.java
│  ├─ Demographic.java
│  ├─ FetchTop.java
│  ├─ Genre.java
│  ├─ Parser.java
│  └─ Producer.java
├─ Data/
│  ├─ Anime.java
│  ├─ Demographic.java
│  ├─ Genre.java
│  ├─ Producer.java
│  ├─ UserAnimeStat.java
│  ├─ Users.java
│  └─ UserStat.java
├─ Exeptions/           
│  └─ HttpRequestException.java
├─ Mapper/
│  ├─ AnimeMapper.java
│  ├─ UserAnimeStatMapper.java
│  ├─ UserMapper.java
│  └─ UserStatMapper.java
├─ Scripts/
│  ├─ DataIntegrityRestorer.java
│  ├─ FetchAndPersist.java
│  └─ UserResyncService.java
├─ UserParsing/
│  ├─ CategoryStats.java
│  ├─ FetchUsers.java
│  ├─ Parser.java
│  ├─ StatsData.java
│  ├─ StatsResponse.java
│  ├─ UserAnimeEntry.java
│  └─ UserLite.java
├─ Utils/
│  ├─ DateTime.java
│  ├─ OkHttpClientManager.java
│  └─ SchemaGenerator.java
└─ Main.java
```


# Структура базы данных

**Основные сущности:**

- `anime` — метаданные аниме  
- `producer`, `genre`, `demographic` — справочники, связанные с `anime` через M:N  
- `users` — профиль пользователя MAL  
- `user_stat` — агрегированные статистики пользователя (one-to-one)  
- `user_anime_stat` — взаимодействие пользователя с аниме (оценка / статус) — composite PK (`user_id`, `anime_id`)  
- join-таблицы: `anime_producer`, `anime_licensor`, `anime_studio`, `anime_genre`, `anime_theme`, `anime_demographic`

---

## Таблица `anime`

| Колонка | Тип (Postgres) | Описание |
|---|---:|---|
| **mal_id** | `integer PRIMARY KEY` | id из MAL |
| **url** | `varchar(1024) NOT NULL` | ссылка на страницу |
| **status** | `varchar(128)` | статус (e.g. Finished) |
| **title** | `varchar(512) NOT NULL` | оригинальное название |
| **approved** | `boolean` | флаг валидности/проверки |
| **title_english** | `varchar(512)` | английское название |
| **title_japanese** | `varchar(512)` | японское название |
| **type** | `varchar(64)` | TV / Movie / OVA |
| **episodes** | `integer` | число эпизодов |
| **rating** | `varchar(64)` | возрастной рейтинг |
| **score** | `double precision` | средний балл |
| **scored_by** | `integer` | количество голосов |
| **synopsis** | `text` | описание |
| **background** | `text` | background |
| **season** | `varchar(32)` | сезон (Spring/Fall...) |
| **year** | `integer` | год релиза |

---

## Справочники (`producer`, `genre`, `demographic`)

### `producer`

| Колонка | Тип | Описание |
|---|---:|---|
| **mal_id** | `integer PRIMARY KEY` | id производителя |
| **type** | `varchar NOT NULL` | тип (company, studio и т.д.) |
| **name** | `varchar NOT NULL` | название |
| **url** | `varchar` | ссылка |

### `genre`

| Колонка | Тип | Описание |
|---|---:|---|
| **mal_id** | `integer PRIMARY KEY` | 
| **type** | `varchar NOT NULL` | 
| **name** | `varchar NOT NULL` | 
| **url** | `varchar` | 

### `demographic`

| Колонка | Тип | Описание |
|---|---:|---|
| **mal_id** | `integer PRIMARY KEY` | 
| **name** | `varchar NOT NULL` | 
| **url** | `varchar` | 


