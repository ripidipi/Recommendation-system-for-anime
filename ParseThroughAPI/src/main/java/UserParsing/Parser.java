package UserParsing;

import Data.UserStat;
import Data.Users;
import Mapper.UserAnimeStatMapper;
import jakarta.persistence.*;
import Mapper.UserMapper;
import Mapper.UserStatMapper;

import java.io.IOException;


public class Parser {

    private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("animePU");

    public static void saveUserAndStats(UserLite dto) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            Users user = UserMapper.mapOrCreate(dto, em);

            StatsData stats = FetchUsers.fetchUserStats(dto.username);

            AnimeListPersist(dto, em);

            UserStat userStats = UserStatMapper.mapOrCreate(stats, user, em);
            em.merge(userStats);

            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Error saving user and stats: " + dto.username, e);
        } finally {
            em.close();
        }
    }

    public static void saveUserAndStats(UserLite dto, StatsData stats) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            Users user = UserMapper.mapOrCreate(dto, em);

            AnimeListPersist(dto, em);

            UserStat userStats = UserStatMapper.mapOrCreate(stats, user, em);
            em.merge(userStats);

            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Error saving user and stats: " + dto.username, e);
        } finally {
            em.close();
        }
    }


    private static void AnimeListPersist(UserLite dto, EntityManager em) throws IOException, InterruptedException {
        final int batchSize = 100;
        final int[] counter = {0};

        boolean ok = FetchUsers.fetchUserAnimeList(dto.username, page -> {
            for (UserAnimeEntry entry : page) {
                UserAnimeStatMapper.mapAndCreate(entry, dto.malId, em);
                if (++counter[0] % batchSize == 0) {
                    em.flush();
                }
            }
            try {
                em.flush();
            } catch (Exception e) {
                System.out.println("Flush error for user " + dto.username + ": " + e.getMessage());
            }
        });
        if (!ok) {
            System.out.println("Warning: partial data for user " + dto.username + " (fetch returned false).");
        }
    }

}
