package Mapper;

import UserParsing.UserAnimeEntry;
import Data.UserAnimeStat;
import Utils.DateTime;
import jakarta.persistence.EntityManager;

import java.time.OffsetDateTime;

public class UserAnimeStatMapper {

    private static String statusToString(Integer code) {
        if (code == null) return null;
        switch (code) {
            case 1: return "watching";
            case 2: return "completed";
            case 3: return "on_hold";
            case 4: return "dropped";
            case 6: return "plan_to_watch";
            default: return "status_" + code;
        }
    }

    public static void mapAndCreate(UserAnimeEntry dto, int userId, EntityManager em) {
        if (dto == null || dto.animeId == null) return;

        Data.Anime animeEntity = em.find(Data.Anime.class, dto.animeId);
        if (animeEntity == null) {
            animeEntity = new Data.Anime();
            animeEntity.setMalId(dto.animeId);
            em.persist(animeEntity);
        }

        UserAnimeStat.UserAnimeKey key = new UserAnimeStat.UserAnimeKey();
        key.setUserId(userId);
        key.setAnimeId(dto.animeId);

        UserAnimeStat entity = em.find(UserAnimeStat.class, key);
        if (entity == null) {
            entity = new UserAnimeStat();
            entity.setUserId(userId);
            entity.setAnimeId(dto.animeId);
        }

        Integer scoreToSet = (dto.score != null && dto.score > 0) ? dto.score : null;
        entity.setScore(scoreToSet);
        entity.setStatus(statusToString(dto.status));
        entity.setEpisodesWatched(dto.numWatchedEpisodes);

        OffsetDateTime last = null;
        if (dto.updatedAt != null) last = DateTime.parseToOffsetDateTime(dto.updatedAt);
        else if (dto.createdAt != null) last = DateTime.parseToOffsetDateTime(dto.createdAt);
        entity.setLastUpdated(last != null ? last : OffsetDateTime.now());

        em.merge(entity);
    }

}
