# ParseThroughAPI — Java-парсер MAL → PostgreSQL

**Коротко:** парсер собирает данные с MyAnimeList (через Jikan / парсинг), сохраняет их в PostgreSQL (Hibernate JPA + HikariCP), выполняет валидацию и восстановление целостности, и помечает/отсеивает неполные записи на этапе экспорта для ML.

## Структура
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
