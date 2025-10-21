package mapper;

import data.Users;
import user_parsing.StatsData;
import data.UserStat;
import jakarta.persistence.EntityManager;


public class UserStatMapper {

    private UserStatMapper() {}

    /**
     * @return managed entity (persisted if newly created)
     */
    public static UserStat mapOrCreate(StatsData dto, Users user, EntityManager em) {
        UserStat stats = user.getUserStat();

        if (stats == null) {
            stats = new UserStat();
            stats.setUser(user);
            user.setUserStat(stats);
            em.persist(stats);
        }

        apply(stats, dto);
        return stats;
    }

    private static void apply(UserStat entity, StatsData dto) {
        entity.setDaysWatched(dto.anime.daysWatched);
        entity.setMeanScore(dto.anime.meanScore);
        entity.setWatching(dto.anime.watching);
        entity.setCompleted(dto.anime.completed);
        entity.setOnHold(dto.anime.onHold);
        entity.setDropped(dto.anime.dropped);
        entity.setPlanToWatch(dto.anime.planToWatch);
        entity.setTotalEntries(dto.anime.totalEntries);
        entity.setRewatched(dto.anime.rewatched);
        entity.setEpisodesWatched(dto.anime.episodesWatched);
    }
}

