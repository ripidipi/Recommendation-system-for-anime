package mapper;

import data.Users;
import user_parsing.StatsData;
import data.UserStat;
import jakarta.persistence.EntityManager;


public class UserStatMapper {

    public static UserStat mapOrCreate(StatsData dto, Users user, EntityManager em) {
        UserStat stats = em.createQuery(
                        "SELECT s FROM UserStat s WHERE s.user = :u", UserStat.class)
                .setParameter("u", user)
                .getResultStream()
                .findFirst()
                .orElseGet(() -> {
                    UserStat s = new UserStat();
                    s.setUser(user);
                    return s;
                });

        stats.setDaysWatched(dto.anime.daysWatched);
        stats.setMeanScore(dto.anime.meanScore);
        stats.setWatching(dto.anime.watching);
        stats.setCompleted(dto.anime.completed);
        stats.setOnHold(dto.anime.onHold);
        stats.setDropped(dto.anime.dropped);
        stats.setPlanToWatch(dto.anime.planToWatch);
        stats.setTotalEntries(dto.anime.totalEntries);
        stats.setRewatched(dto.anime.rewatched);
        stats.setEpisodesWatched(dto.anime.episodesWatched);

        return stats;
    }
}

