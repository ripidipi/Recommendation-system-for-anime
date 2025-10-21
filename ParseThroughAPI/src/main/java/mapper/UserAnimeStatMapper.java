package mapper;

import user_parsing.UserAnimeEntry;
import data.UserAnimeStat;
import utils.DateTime;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.Map;

public final class UserAnimeStatMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserAnimeStatMapper.class);

    private static final Map<Integer, String> STATUS_MAP = Map.of(
            1, "watching",
            2, "completed",
            3, "on_hold",
            4, "dropped",
            6, "plan_to_watch"
    );

    private UserAnimeStatMapper() {}

    private static String statusToString(Integer code) {
        if (code == null) return null;
        return STATUS_MAP.getOrDefault(code, "status_" + code);
    }

    public static void map(UserAnimeEntry dto, int userId, EntityManager em) {

        if (dto == null || dto.animeId == null) {
            return;
        }

        data.Anime animeEntity = em.find(data.Anime.class, dto.animeId);
        if (animeEntity == null) {
            LOGGER.warn("Skipping anime with malId={} - not found in database", dto.animeId);
            return;
        }

        UserAnimeStat.UserAnimeKey key = new UserAnimeStat.UserAnimeKey();
        key.setUserId(userId);
        key.setAnimeId(dto.animeId);

        UserAnimeStat entity = em.find(UserAnimeStat.class, key);

        if (entity == null) {
            entity = new UserAnimeStat();
            entity.setUserId(userId);
            entity.setAnimeId(dto.animeId);
            em.persist(entity);
        }
        apply(entity, dto);
    }

    private static void apply(UserAnimeStat entity, UserAnimeEntry dto) {
        Integer scoreToSet = (dto.score != null && dto.score > 0) ? dto.score : null;
        entity.setScore(scoreToSet);

        entity.setStatus(statusToString(dto.status));
        entity.setEpisodesWatched(dto.numWatchedEpisodes);

        OffsetDateTime last = null;
        try {
            if (dto.updatedAt != null) {
                last = DateTime.parseToOffsetDateTime(dto.updatedAt);
            } else if (dto.createdAt != null) {
                last = DateTime.parseToOffsetDateTime(dto.createdAt);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to parse dates for userAnime (animeId={}, userId={}), will use now(): {}",
                    entity.getAnimeId(), entity.getUserId(), e.getMessage());
        }
        entity.setLastUpdated(last != null ? last : OffsetDateTime.now());
    }
}
