package Mapper;

import Data.Users;
import UserParsing.StatsData;
import Data.UserAnimeStat;
import jakarta.persistence.EntityManager;


public class UserAnimeStatMapper {

    public static UserAnimeStat mapOrCreate(StatsData dto, Users user, EntityManager em) {
        UserAnimeStat stats = em.createQuery(
                        "SELECT s FROM UserAnimeStat s WHERE s.user = :u", UserAnimeStat.class)
                .setParameter("u", user)
                .getResultStream()
                .findFirst()
                .orElseGet(() -> {
                    UserAnimeStat s = new UserAnimeStat();
                    s.setUser(user);
                    return s;
                });

        stats.setDaysWatched(dto.anime.days_watched);
        stats.setMeanScore(dto.anime.mean_score);
        stats.setWatching(dto.anime.watching);
        stats.setCompleted(dto.anime.completed);
        stats.setOnHold(dto.anime.on_hold);
        stats.setDropped(dto.anime.dropped);
        stats.setPlanToWatch(dto.anime.plan_to_watch);
        stats.setTotalEntries(dto.anime.total_entries);
        stats.setRewatched(dto.anime.rewatched);
        stats.setEpisodesWatched(dto.anime.episodes_watched);

        return stats;
    }
}

